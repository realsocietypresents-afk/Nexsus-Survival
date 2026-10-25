#!/bin/bash
# Compiles the hand-written Bukkit stubs, the parts of NexusSurvival the tests cover and the tests with
# -Xlint:all -Werror, then runs every *Test. (There is no Paper server or Maven here.)
set -e
unset JAVA_TOOL_OPTIONS
cd "$(dirname "$0")/.."
B=${BUILD:-/tmp/nexussurvival-build}
rm -rf "$B"; mkdir -p "$B/stubs" "$B/main" "$B/test"
javac -encoding UTF-8 -nowarn -d "$B/stubs" $(find stubs -name '*.java')
P=src/main/java/com/nexusuniverse/survival
FILES="$(ls $P/config/*.java $P/data/*.java $P/integration/*.java $P/seasons/*.java $P/climate/*.java $P/radiation/RadiationManager.java $P/radiation/RadiationItems.java $P/radiation/RadiationZone.java $P/hygiene/HygieneManager.java $P/thirst/ThirstManager.java $P/disease/Disease.java $P/disease/DiseaseManager.java $P/SurvivalTicker.java) $(find tools/support -name '*.java')"
javac -encoding UTF-8 -Xlint:all -Werror -cp "$B/stubs" -d "$B/main" $FILES
javac -encoding UTF-8 -Xlint:all -Werror -cp "$B/stubs:$B/main" -d "$B/test" $(find tools/tests -name '*.java')
python3 -I - <<'PY'
import re, sys
s = open('src/main/resources/plugin.yml').read()
assert re.search(r'^name: NexusSurvival$', s, re.M), 'plugin.yml name'
assert 'softdepend: [NexusMorality, NexusPath]' in s, 'softdepend'
PY
for t in $(cd tools/tests && find . -name '*Test.java' | sed 's#^\./##; s#\.java$##; s#/#.#g' | sort); do
  echo "== $t"
  java -cp "$B/stubs:$B/main:$B/test" "$t"
done
