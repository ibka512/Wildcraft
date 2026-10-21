# Third-party provenance

- Bootstrap template: FabricMC/fabric-example-mod, Minecraft 26.3 branch, inspected on 2026-10-02. https://github.com/FabricMC/fabric-example-mod/tree/26.3 — CC0-1.0.
- Gradle wrapper scripts and JAR: supplied by that template, Gradle 9.7.1. https://gradle.org/ — Apache-2.0; the scripts include the copyright and license notice.
- Fabric Loader and Fabric API remain external dependencies and retain their licenses. Their nested libraries retain their own notices.
- Minecraft game files are downloaded by the development tools; they are not part of the Wildcraft source distribution.
- The test core pixel artwork is original project artwork.
- Core art v1 was supplied by the project owner in `Wildcraft-核心美术成果-2026-10-03.zip` (2026-10-03); source provenance and byte hashes are in `art/approved-v1/SOURCE-MANIFEST.json`. Blender reference scenes do not replace or distribute Minecraft item models; runtime uses the installed game models.

- Optional local-only first-person sword arms are derived from the owner-adopted `first-person-sword/v01` source. Provenance and the mixed upstream chain are retained in `art/player-animation-v1/first-person-sword/v01/licenses/SOURCE.md`; this source inherits the earlier sword/idle chain and is not represented as wholly CC0. The original game skin and item models are resolved from the installed game. Derived player-animation data and binaries containing it remain local and are excluded from the public source snapshot.

- Optional local-only heavy-sword/axe first-person data retain the mixed upstream licence chain. Four additional mechanical bottom variants were authorized only for local integration. Their original proposals and derived geometry are excluded from the public snapshot. Conversion code and adoption scope/hash records do not grant redistribution rights to the missing assets.
