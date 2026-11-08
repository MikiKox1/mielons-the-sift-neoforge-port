"""Exercise the small local-only RCON parser without connecting to Minecraft."""
from pathlib import Path
import importlib.util
import socket
import struct
import unittest

SPEC = importlib.util.spec_from_file_location("probe", Path(__file__).resolve().parents[1] / "tools/probe_disposable_world.py")
PROBE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(PROBE)


class ProbeProtocol(unittest.TestCase):
    def test_echoed_marker_in_command_error_is_not_a_pass(self):
        with self.assertRaises(AssertionError):
            PROBE.validate_response("Incorrect argument for command\n...MARKER<--[HERE]", "MARKER")
        PROBE.validate_response("[Rcon] MARKER", "MARKER")

    def test_utf8_round_trip(self):
        for text in ("", "test", "Próba Świata"):
            sender, receiver = socket.socketpair()
            try:
                sender.sendall(PROBE.packet(12, 0, text))
                self.assertEqual(PROBE.receive(receiver), (12, 0, text))
            finally:
                sender.close()
                receiver.close()

    def test_invalid_lengths_rejected(self):
        for size in (-1, 0, 9, 1048577):
            sender, receiver = socket.socketpair()
            try:
                sender.sendall(struct.pack("<i", size))
                with self.assertRaises(ValueError):
                    PROBE.receive(receiver)
            finally:
                sender.close()
                receiver.close()


if __name__ == "__main__":
    unittest.main()
