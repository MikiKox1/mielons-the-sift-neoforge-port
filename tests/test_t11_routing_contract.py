"""Regression for the user-observed wrong portal-to-Rift return.

These source-level gates complement, but do not replace, the real server GameTests.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "src/main/java/mielon/thesift/world"


class T11RouteContract(unittest.TestCase):
    def test_recorded_underground_portal_never_uses_surface_spawn_finder(self):
        source = (SOURCE / "SiftTeleportManager.java").read_text(encoding="utf-8")
        method = source.split("private static boolean teleportBackToOverworld(", 1)[1].split(
            "private static Optional<BlockPos> findActiveEntranceNearby(", 1
        )[0]
        self.assertNotIn("entity.adjustSpawnLocation(overworld, entrance)", method)
        self.assertIn("SafePortalArrival.findNearRecordedPortal(", method)
        self.assertIn("Sift MAIN return: no safe landing within saved portal level", method)
        self.assertNotIn("Sift MAIN return: no safe spot near saved entrance", method)

    def test_main_portal_never_queues_a_rift_exit(self):
        source = (SOURCE / "SiftTeleportManager.java").read_text(encoding="utf-8")
        method = source.split("private static boolean teleportBackToOverworld(", 1)[1].split(
            "private static Entity teleport(", 1
        )[0]
        self.assertNotIn("RiftManager.returnFromPortal(", method)
        self.assertIn("SiftWorldStorage.getMainPortalEntrance(", method)
        self.assertIn("SafePortalArrival.find(", method)

    def test_durable_per_entity_main_portal_is_not_erased_by_rift(self):
        source = (SOURCE / "SiftWorldStorage.java").read_text(encoding="utf-8")
        self.assertIn('MAIN_PORTAL_ENTRANCES = "main_portal_entrances"', source)
        self.assertIn("public static Optional<SiftWorldStorage.ReturnPoint> getMainPortalEntrance(", source)
        saved = source.split("public static void saveReturnPoint(", 1)[1].split(
            "public static Optional<SiftWorldStorage.ReturnPoint> getReturnPoint(", 1
        )[0]
        self.assertIn("MAIN_PORTAL_ENTRANCES", saved)
        cleared = source.split("public static void clearReturnPoint(", 1)[1].split(
            "public static void markPortalExitRequired(", 1
        )[0]
        self.assertIn("preserveMainPortalEntrance(", cleared)
        mark = source.split("public static void markRiftEntry(", 1)[1].split(
            "public static void saveRiftRoute(", 1
        )[0]
        self.assertIn("clearReturnPoint(", mark)
        self.assertNotIn("removeEntry(MAIN_PORTAL_ENTRANCES", mark)

    def test_main_portal_return_resolves_nearby_active_frame(self):
        source = (SOURCE / "SiftTeleportManager.java").read_text(encoding="utf-8")
        self.assertIn("findActiveEntranceNearby(", source)
        self.assertIn("SiftPortalBlock.isActivePortal(", source)

    def test_rift_entry_does_not_end_inside_main_portal(self):
        source = (SOURCE / "RiftManager.java").read_text(encoding="utf-8")
        method = source.split("private static RiftManager.Destination intoSift(", 1)[1].split(
            "private static RiftManager.Destination outOfSift(", 1
        )[0]
        self.assertNotIn("SiftPortalStructure.ensurePortal(", method)
        self.assertIn("origin.getLinkedPos()", method)
        self.assertIn("new RiftManager.Destination(sift", method)

    def test_successful_portal_and_rift_returns_clear_passenger_tickets(self):
        main = (SOURCE / "SiftTeleportManager.java").read_text(encoding="utf-8")
        main_return = main.split("private static boolean teleportBackToOverworld(", 1)[1].split(
            "private static Optional<BlockPos> findActiveEntranceNearby(", 1
        )[0]
        self.assertIn("teleported.getSelfAndPassengers().forEach", main_return)
        self.assertIn("SiftWorldStorage.clearReturnPoint(server, passenger.getUUID())", main_return)
        rift = (SOURCE / "RiftManager.java").read_text(encoding="utf-8")
        self.assertIn("moved.getSelfAndPassengers().forEach", rift)
        self.assertIn("SiftWorldStorage.clearReturnPoint(server, passenger.getUUID())", rift)

    def test_root_vehicle_uses_passenger_entrance_if_root_has_no_ticket(self):
        source = (SOURCE / "SiftTeleportManager.java").read_text(encoding="utf-8")
        main_return = source.split("private static boolean teleportBackToOverworld(", 1)[1].split(
            "private static Optional<BlockPos> findActiveEntranceNearby(", 1
        )[0]
        self.assertIn("entity.getSelfAndPassengers().toList()", main_return)
        self.assertIn("SiftWorldStorage.getReturnPoint(server, traveler.getUUID())", main_return)
        self.assertIn("SiftWorldStorage.getMainPortalEntrance(server, traveler.getUUID())", main_return)

    def test_linked_rift_is_validated_only_after_entity_sections_are_loaded(self):
        rift = (SOURCE / "RiftManager.java").read_text(encoding="utf-8")
        self.assertIn("areEntitiesLoaded(", rift)
        self.assertGreaterEqual(rift.count("areEntitiesLoaded("), 2)
        self.assertGreaterEqual(rift.count("getEntitiesOfClass(RiftEntity.class"), 1)
        self.assertIn("RiftDirectory.loaded().stream().anyMatch(rift -> matchesLinkedRift", rift)
        self.assertIn("matchesOverworldRift", rift)
        self.assertIn("if (link.isEmpty()) {", rift)
        self.assertIn("EXIT_SEARCHES.computeIfAbsent", rift)


if __name__ == "__main__":
    unittest.main()
