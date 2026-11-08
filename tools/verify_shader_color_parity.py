"""Execute original and new eight-layer color math on the same deterministic grid."""
from pathlib import Path
import argparse
import hashlib
import json
import re
import subprocess

BASES={'26.1.2':'693b2fae4f3c45d491067138e39730eb7133a7bf',
       '26.2':'cfc09025c4a9d5dc4de86f248f83370c23c0a2fb',
       '26.3':'b23395351ff39c02acd4682fce4d6b6a5130e5d5'}
JDK=Path(r'C:\Program Files\Java\jdk-25.0.4.1\bin')


def block(text,start):
    opening=text.index('{',start);depth=1
    for i in range(opening+1,len(text)):
        depth+=(text[i]=='{')-(text[i]=='}')
        if not depth:return text[start:i+1]
    raise ValueError('Unclosed Java block')


def methods(text,name):
    starts=[m.start() for m in re.finditer(r'   (?:private )?static [^\n]+ '+name+r'\(',text)]
    if not starts:raise ValueError('Missing actual production method '+name)
    return '\n'.join(block(text,start) for start in starts)


def sampler(text,kind,mc):
    # Keep the actual production geometry/color constants when a target uses
    # names instead of the upstream renderer's inlined numeric literals.
    fields=re.findall(r'   private static final (?:float|int) [A-Z_]+\s*=\s*[^;\n]+;',text)
    for name in ('DAY_PALETTE','NIGHT_PALETTE'):
        start=text.index('   private static final float[][] '+name)
        fields.append(block(text,start)+';')
    funcs=[methods(text,name) for name in ('baseColor','convergeLowerHemisphere','ribbonColor','channel','lerp','smooth','clamp')]
    if kind=='Baseline':
        funcs.append(methods(text,'addVertex'))
        invoke='BufferBuilder out = new BufferBuilder(); addVertex(out, u, v, night, layer); return out.pixel;'
        stub='''static final class BufferBuilder {
            int pixel;
            BufferBuilder addVertex(float x,float y,float z){return this;}
            BufferBuilder setColor(int r,int g,int b,int a){pixel=(r<<24)|(g<<16)|(b<<8)|a;return this;}
        }'''
    else:
        name='sampleColor' if mc=='26.1.2' else 'shaderLayerColor'
        funcs.append(methods(text,name))
        expression='sampleColor(u,v,night,layer)' if mc=='26.1.2' else 'shaderLayerColor(textureLayer,u,v)'
        invoke='float[] rgba='+expression+'; for(float c:rgba) if(!Float.isFinite(c)) throw new AssertionError("Nonfinite channel"); return (channel(rgba[0])<<24)|(channel(rgba[1])<<16)|(channel(rgba[2])<<8)|channel(rgba[3]);'
        stub=''
    return ('import mielon.thesift.world.ThriveSkyColors;\npublic final class '+kind+' {\n'
            +'\n'.join(fields+funcs)+'\n'+stub+'\nstatic int sample(int textureLayer,float u,float v) {'
            +'boolean night=textureLayer==1||textureLayer==3||textureLayer==5;'
            +'int layer=textureLayer<6?textureLayer/2:textureLayer-3;'+invoke+'}\n}\n')


HARNESS='''public final class ColorParity {
    public static void main(String[] args) {
        long count=0;
        for(int layer=0;layer<8;layer++) for(int y=0;y<=128;y++) for(int x=0;x<=256;x++) {
            check(layer,x/256.0f,y/128.0f);count++;
        }
        java.util.Random random=new java.util.Random(20261108L);
        for(int i=0;i<10000;i++){check(i%8,random.nextFloat(),random.nextFloat());count++;}
        System.out.println("COLOR_PARITY_SAMPLES="+count);
        System.out.println("COLOR_PARITY_CHANNEL_ASSERTIONS="+(count*4));
        System.out.println("COLOR_PARITY=PASS");
    }
    private static void check(int layer,float u,float v) {
        int a=Baseline.sample(layer,u,v),b=Candidate.sample(layer,u,v);
        if(a!=b)throw new AssertionError("Color drift layer="+layer+" u="+u+" v="+v+" original="+Integer.toHexString(a)+" candidate="+Integer.toHexString(b));
    }
}'''


def main():
    p=argparse.ArgumentParser();p.add_argument('--worktree',type=Path,default=Path(__file__).resolve().parents[1]);args=p.parse_args()
    root=args.worktree.resolve();props=dict(l.split('=',1) for l in (root/'gradle.properties').read_text().splitlines() if '=' in l and not l.startswith('#'))
    mc=props['minecraft_version'];base=BASES[mc]
    relative='src/client/java/mielon/thesift/client/render/SiftProceduralSkyRenderer.java'
    before=subprocess.check_output(['git','-C',str(root),'show',base+':'+relative],text=True,encoding='utf8')
    after=(root/relative).read_text(encoding='utf8')
    temp=root/'build/shader-color-parity';temp.mkdir(exist_ok=True,parents=True)
    (temp/'Baseline.java').write_text(sampler(before,'Baseline',mc),encoding='utf8')
    (temp/'Candidate.java').write_text(sampler(after,'Candidate',mc),encoding='utf8')
    (temp/'ColorParity.java').write_text(HARNESS,encoding='utf8')
    thrive=root/'src/main/java/mielon/thesift/world/ThriveSkyColors.java'
    subprocess.run([str(JDK/'javac.exe'),'-encoding','UTF-8','-d',str(temp),str(thrive),*[str(temp/n) for n in ('Baseline.java','Candidate.java','ColorParity.java')]],check=True)
    result=subprocess.run([str(JDK/'java.exe'),'-cp',str(temp),'ColorParity'],capture_output=True,text=True,check=True)
    print(mc,result.stdout)
    assert 'COLOR_PARITY=PASS' in result.stdout
    evidence=root/'evidence';evidence.mkdir(exist_ok=True)
    (evidence/'SHADERS-COLOR-PARITY.json').write_text(json.dumps({'minecraft':mc,'base_commit':base,
        'source_sha256':hashlib.sha256(after.encode()).hexdigest(),'result':result.stdout.splitlines(),
        'scope':'Executed byte-quantized eight-layer original vertex colors vs actual new UV sampler; not a visual-render acceptance'},indent=2)+'\n',encoding='utf8')


if __name__=='__main__':main()
