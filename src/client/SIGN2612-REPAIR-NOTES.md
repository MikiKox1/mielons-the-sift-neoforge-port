# The Sift 2.0.1 / MC26.1.2 — Willow sign GUI i block models

**Zakres worker-7, rozszerzony za zgodą Prime:** dwa `WoodType` i osiem konstruktorów sign w `ModBlocks.java`; `Sheets.addWoodType` w `TheSiftClient.java`; dwa Willow GUI mixiny; `assets/the_sift/{blockstates,models}/**/*sign*.json`; 4 nowe `textures/entity/signs/**` PNG; własne skrypty i testy. Nie zmieniono Gradle, innych konstruktorów w `ModBlocks`, metadanych, konfiguracji mixinów ani oryginalnych PNG. Baza: zaakceptowany MC26.2 ALPHA5 `22176ab7e71ad9a5c2c624cfc72015702bb98f22`.

## Incydent P0 i potwierdzenie przyczyny

- `PORT2612-CLIENT-OPENGL-PASS1.log`: crash klienta przy logowaniu, `InvalidMixinException: @Shadow field texture was not located in ...SignEditScreen`, z `WillowSignEditScreenMixin`.
- Exact `build/moddev/artifacts/minecraft-patched-26.1.2.126-sources.jar`: `SignEditScreen` **nie ma** pola `texture`. Metoda `extractSignBackground(GuiGraphicsExtractor)` używa `StandingSignRenderer.createSignModel` oraz `graphics.sign(model, ..., woodType)`. Poprzedni mixin odziedziczył niezgodne pole z 26.2.
- `HangingSignEditScreen` **ma** `private final Identifier texture`; jego mixin z `@Shadow @Final @Mutable` działa z sygnaturą targetu i zachowuje obie odmiany oraz wall/ceiling. Nie wyciszono żadnego mixinu przez `require=0`.
- W logu jeszcze przed crashem wystąpiło **14** ostrzeżeń brakujących `minecraft:block/template_sign_rot_{0..3}`, `template_hanging_sign_rot_{0..3}`, `template_attached_hanging_sign_rot_{0..3}`, `template_wall_sign`, `template_wall_hanging_sign`. Takich modeli nie ma w exact target26.1.2.

## Zastosowana naprawa GUI, blockstates i in-world

1. `WillowSignEditScreenMixin.java`: usunięto niedozwolony `@Shadow texture`. Wymagany `@Inject` ctor zapisuje wybrany niestandardowy `WoodType`, a `@ModifyArg` w rzeczywistym `SignEditScreen.extractSignBackground` wskazuje go jako 3. argument `GuiGraphicsExtractor.sign(Model.Simple,float,WoodType,int,int,int,int)`. Jest to **natywny render 3D** — nie zastępujemy go płaską bitmapą ani Oak. Vanilla signs zachowują niezmieniony `WoodType`; tekst, zapis i GUI pozostają w `AbstractSignEditScreen`.
2. Osiem `assets/the_sift/blockstates/*willow*sign.json` (2 rodzaje drewna × standing/wall/hanging/wall_hanging) ma teraz ten sam, rzeczywisty schemat co MC26.1.2 `oak_*sign`: pojedynczy wariant `""`, odsyłający do cząstkowego modelu danej odmiany drewna. Obrót i 3D tabliczki obsługuje już `StandingSignRenderer` lub `HangingSignRenderer` bloku-encji, nie stary baked block model.
3. Dodano 4 kanoniczne modele `models/block/{overgrown_willow,ash_willow}_{sign,hanging_sign}.json` wyłącznie z `textures.particle`. Dla standing — odpowiednie custom planks, hanging — odpowiedni stripped Willow log. Zachowano 28 istniejących nazw modeli rot/wall, ale również przeniesiono je na poprawne particle-only, dzięki czemu żadna ich definicja nie odwołuje się już do 14 nieistniejących vanilla `template_*`. Nie dodano fałszywej geometrii.
4. `ModBlocks.java`: `WoodType.register(new WoodType("the_sift:overgrown_willow", BlockSetType.OAK))` oraz odpowiednik `the_sift:ash_willow`. **Wszystkie 8** konstruktorów `StandingSignBlock`, `WallSignBlock`, `CeilingHangingSignBlock` i `WallHangingSignBlock` używa wyłącznie własnego typu. `BlockSetType.OAK` zachowuje mechanikę/dźwięki, ale nie wymusza tekstury Oak. Nie zmieniano innych bloków ani fence gates.
5. `TheSiftClient.initialize()`, wywoływane przez `FMLClientSetupEvent.enqueueWork`: `Sheets.addWoodType` rejestruje oba nowe sprite'y. **Nie dodajemy** ręcznych eventów warstw, ponieważ exact target `LayerDefinitions.createRoots` (linie 561–571) automatycznie tworzy standing, wall i trzy hanging layers dla wszystkich `WoodType.values()`. Osobny event dublowałby klucze model layer.
6. `tools/convert_sign_textures_2612.py` deterministycznie mapuje piksele oryginalnych, niezmutowanych 2.0.1 tekstur **32×32** na cztery nowe arkusze entity `textures/entity/signs/{overgrown_willow,ash_willow}.png` oraz `hanging/{...}.png` w docelowym UV **64×32**. Oryginały są pinowane dokładnym SHA JAR Fabric262; bez interpolacji, generowania nowych kolorów ani kopiowania Oak. UV stojącej deski (źródło `(0,16)–(24,28)` → cel `(2,2)–(26,14)`), wiszącej (źródło `(18,16)–(32,26)` → cel `(2,14)–(16,24)`) są testowane bajtowo. Boki/deski, uchwyty i łańcuchy mają rozpisane mapowania w generatorze. Model geometryczny pozostaje natywny MC26.1.2.
7. Atlas target `minecraft:signs`: jego wbudowane `assets/minecraft/atlases/signs.json` zawiera `minecraft:directory` ze źródłem `entity/signs`; `DirectoryLister` skanuje wszystkie namespace i rozpoznaje `the_sift:entity/signs/*`. **Nowy `assets/minecraft/atlases/signs.json` nie jest potrzebny** i mógłby nieumyślnie nadpisać vanilla atlas. `Sheets.SIGN_MAPPER` i `HANGING_SIGN_MAPPER` wiążą własne WoodType z tymi plikami.

## RED → GREEN (tylko statyczna bramka)

| Bramka | Wcześniej | Po zmianie |
|---|---|---|
| `py -3.14 -B -m unittest discover -s tests -p test_sign_2612_compat.py -v` | 7 początkowych testów: 2 FAIL + 1 ERROR, pozostałe 4 PASS | **10/10 PASS** (dodatkowo custom WoodType, native sprites/layers i UV2.0.1) |
| `py -3.14 -B -m unittest discover -s tests -p test_target_model_references_2612.py -v` | Prime wcześniej zgłosił RED: 28 custom-parentów → 14 brakujących vanilla templates | **2/2 PASS** |
| `py -3.14 -B tools/convert_sign_textures_2612.py` | Brak nowych PNG | **4/4 SHA/UV identyczne z deterministycznym generatorem** |

Potwierdzono istnienie4cząstkowych tekstur,4źródeł32×32 i4gotowych entityPNG64×32. **Późniejszy rzeczywisty test Prime PASS:** klient i serwer uruchomione po poprawce; oba materiały stojących/wiszących Willow widoczne bez Oak. Natywne GUI zwykłego Overgrown oraz wiszącego Ash otwarto, wpisano tekst i odczytano go z serwera front_text/back_text. F3+T oraz wejście do Sift z efektami nie wywołały błędów; kontrolowane zamknięcie exit0. Dowód: `PORT2612-CLIENT-OPENGL-PASS2.log` i trzy `evidence/MC2612-Willow-*.png`. Pełny ręczny przegląd wszystkich8wariantów jest nadal osobnym M2612-01, nie automatycznym PASS właściciela.

## Rozwiązany problem Oak i pozostała bramka runtime

`ModBlocks` przekazuje już dedykowany `WoodType` każdego Willow do wszystkich stojących i wiszących bloków. Target `AbstractSignEditScreen`, `StandingSignRenderer` i `HangingSignRenderer` używają go do pobrania właściwego sprite przez `Sheets.getSignSprite/getHangingSignSprite`. Nazwy obu typów są namespaced `the_sift:...`, a nie `oak`; materiały atlasa pochodzą z prawdziwych 2.0.1 sprite'ów. Zarówno GUI, jak i świat są zatem podłączone statycznie do własnych materiałów, bez fallbacku na dębowy atlas.

**Potwierdzone przez Prime w rzeczywistym kliencie:** własne warstwy/sprite'y obu materiałów, stojąca i wisząca geometria z łańcuchami, obie ścieżki GUI i zapis tekstu. Pozostałe warianty obrotów, ręczne barwienie/glow i ponowne otwarcie świata opisano w24przypadkach właściciela; smoke check nie jest twierdzeniem o sprawdzeniu każdej kombinacji.

## Pochodzenie

`SIGN-2612-ADAPTATIONS.json` zawiera44zasoby:36zmienionych JSON i8nowych modeli/PNG, każdy zpath/hash/change/reason, oraz4oryginalne wejściowePNG. Końcowy manifest sporządził Prime po niezależnej weryfikacji względem zaakceptowanego262JAR i scalił z `TARGET-2612-PROVENANCE.json` i `RESOURCE-2612-ADAPTATIONS.json`. Kod Java i skrypty identyfikuje końcowy Git commit. Żaden oryginalny zasób nie został usunięty.
