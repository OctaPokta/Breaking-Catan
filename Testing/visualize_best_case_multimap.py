"""
Runs the Java RoadTestScenarios harness (several random Catan boards) and renders the
BEST-CASE result for 3 of them (the auto-selected top-ranked strategy for each map) side
by side, horizontally, in a single combined image - so you can see how the algorithm's
best pick changes across different random maps.

Usage: python visualize_best_case_multimap.py
Requires: a JDK on PATH (javac/java), matplotlib.
"""

import math
import re
import subprocess
import sys
from pathlib import Path

import matplotlib
matplotlib.use("Agg")  # headless - we save PNGs instead of showing a window
import matplotlib.pyplot as plt
from matplotlib.patches import RegularPolygon

# ---------------------------------------------------------------------------
# 1. PROJECT PATHS
# ---------------------------------------------------------------------------
PROJECT_ROOT = Path(__file__).resolve().parents[1]   # .../CatanAnalysis
SOURCE_ROOT = PROJECT_ROOT.parent                     # .../ (contains the CatanAnalysis package dir)
BUILD_DIR = PROJECT_ROOT / "testing" / "_build"
OUTPUT_DIR = PROJECT_ROOT / "testing" / "_best_case_multimap"

RESOURCE_NAMES = {0: "Brick", 1: "Ore", 2: "Sheep", 3: "Wheat", 4: "Wood", 5: "Desert"}

# hexToTownsMap copied from Towns.java - for each of the 19 hexes, the 6 town indices
# that touch it (set membership only, order not meaningful)
HEX_TO_TOWNS_MAP = [
    [0,3,4,7,8,12],[1,4,5,8,9,13],[2,5,6,9,10,14],
    [7,11,12,16,17,22],[8,12,13,17,18,23],[9,13,14,18,19,24],[10,14,15,19,20,25],
    [16,21,22,27,28,33],[17,22,23,28,29,34],[18,23,24,29,30,35],[19,24,25,30,31,36],[20,25,26,31,32,37],
    [28,33,34,38,39,43],[29,34,35,39,40,44],[30,35,36,40,41,45],[31,36,37,41,42,46],
    [39,43,44,47,48,51],[40,44,45,48,49,52],[41,45,46,49,50,53],
]

def _build_hex_centers():
    rows = [[0, 1, 2], [3, 4, 5, 6], [7, 8, 9, 10, 11], [12, 13, 14, 15], [16, 17, 18]]
    size = 1.0
    horiz = math.sqrt(3) * size
    vert = 1.5 * size
    centers = {}
    for r, row in enumerate(rows):
        L = len(row)
        for i, h in enumerate(row):
            x = (i - (L - 1) / 2) * horiz
            y = -r * vert
            centers[h] = (x, y)
    return centers

HEX_CENTERS = _build_hex_centers()
HEX_SIZE = 1.0

def _hex_corner_list(cx, cy):
    return [(round(cx + HEX_SIZE * math.cos(math.radians(60 * i - 30)), 4),
             round(cy + HEX_SIZE * math.sin(math.radians(60 * i - 30)), 4)) for i in range(6)]

HEX_CORNERS = {h: _hex_corner_list(*HEX_CENTERS[h]) for h in range(19)}


def _solve_town_positions():
    """Reconstructs the exact (x, y) position of all 54 towns purely from
    HEX_TO_TOWNS_MAP's set-membership data (see visualize_road_scenarios.py for the
    full derivation and why it's the trustworthy source of geometry)."""
    from collections import defaultdict

    town_hexes = defaultdict(list)
    for h in range(19):
        for t in HEX_TO_TOWNS_MAP[h]:
            town_hexes[t].append(h)

    town_position = {}
    claimed = {h: set() for h in range(19)}

    remaining = []
    for t in range(54):
        hexes = town_hexes[t]
        if len(hexes) == 3:
            common = set(HEX_CORNERS[hexes[0]]) & set(HEX_CORNERS[hexes[1]]) & set(HEX_CORNERS[hexes[2]])
            pos = next(iter(common))
            town_position[t] = pos
            for h in hexes:
                claimed[h].add(pos)
        else:
            remaining.append(t)

    changed = True
    while changed and remaining:
        changed = False
        still = []
        for t in remaining:
            hexes = town_hexes[t]
            if len(hexes) == 2:
                shared = list(set(HEX_CORNERS[hexes[0]]) & set(HEX_CORNERS[hexes[1]]))
                free = [c for c in shared if c not in claimed[hexes[0]] and c not in claimed[hexes[1]]]
                if len(free) == 1:
                    town_position[t] = free[0]
                    for h in hexes:
                        claimed[h].add(free[0])
                    changed = True
                    continue
            still.append(t)
        remaining = still

    still = []
    for t in remaining:
        h = town_hexes[t][0]
        free = [c for c in HEX_CORNERS[h] if c not in claimed[h]]
        if len(free) == 1:
            town_position[t] = free[0]
            claimed[h].add(free[0])
        else:
            still.append(t)
    remaining = still

    for h in range(19):
        free_corners = [c for c in HEX_CORNERS[h] if c not in claimed[h]]
        free_towns = sorted(t for t in HEX_TO_TOWNS_MAP[h] if t not in town_position)
        if not free_towns:
            continue
        cx, cy = HEX_CENTERS[h]
        free_corners.sort(key=lambda c: math.atan2(c[1] - cy, c[0] - cx))
        for t, pos in zip(free_towns, free_corners):
            town_position[t] = pos
            claimed[h].add(pos)

    assert len(town_position) == 54 and len(set(town_position.values())) == 54
    return town_position

TOWN_POSITIONS = _solve_town_positions()

def _solve_physical_edges():
    edges = set()
    for h in range(19):
        corner_to_town = {pos: t for t, pos in TOWN_POSITIONS.items() if pos in HEX_CORNERS[h]}
        for i in range(6):
            a, b = HEX_CORNERS[h][i], HEX_CORNERS[h][(i + 1) % 6]
            if a in corner_to_town and b in corner_to_town:
                edges.add(tuple(sorted((corner_to_town[a], corner_to_town[b]))))
    return edges

PHYSICAL_EDGES = _solve_physical_edges()

# ---------------------------------------------------------------------------
# 2. BUILD & RUN THE JAVA HARNESS, CAPTURE ITS OUTPUT
# ---------------------------------------------------------------------------
def run_java_harness() -> str:
    BUILD_DIR.mkdir(parents=True, exist_ok=True)

    java_files = [str(p) for p in SOURCE_ROOT.glob("CatanAnalysis/**/*.java")]
    compile_cmd = ["javac", "-d", str(BUILD_DIR)] + java_files
    result = subprocess.run(compile_cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(result.stdout)
        print(result.stderr)
        sys.exit("Java compilation failed - see errors above.")

    run_cmd = ["java", "-cp", str(BUILD_DIR), "CatanAnalysis.testing.RoadTestScenarios"]
    result = subprocess.run(run_cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(result.stdout)
        print(result.stderr)
        sys.exit("Running RoadTestScenarios failed - see errors above.")

    return result.stdout


# ---------------------------------------------------------------------------
# 3. PARSE THE STRUCTURED OUTPUT INTO SCENARIO DICTS
# ---------------------------------------------------------------------------
def parse_scenarios(raw_output: str):
    scenarios = []
    blocks = re.split(r"=== SCENARIO (\d+) ===", raw_output)[1:]

    for i in range(0, len(blocks), 2):
        scenario_num = int(blocks[i])
        body = blocks[i + 1]

        strategy = re.search(r"STRATEGY:\s*(.+)", body).group(1).strip()
        resources = [int(x) for x in re.search(r"RESOURCES:\s*(.+)", body).group(1).split()]
        dice = [int(x) for x in re.search(r"DICE:\s*(.+)", body).group(1).split()]
        best_town = int(re.search(r"BEST_TOWN:\s*(\d+)", body).group(1))
        road_match = re.search(r"ROAD:\s*(\d+)\s*->\s*(-?\d+)", body)
        road_from, road_to = int(road_match.group(1)), int(road_match.group(2))
        opt_values = [float(x) for x in re.search(r"OPT_VALUES:\s*(.+)", body).group(1).split()]
        tile_values = [float(x) for x in re.search(r"TILE_VALUES:\s*(.+)", body).group(1).split()]

        scenarios.append({
            "num": scenario_num,
            "strategy": strategy,
            "resources": resources,
            "dice": dice,
            "best_town": best_town,
            "road_from": road_from,
            "road_to": road_to,
            "opt_values": opt_values,
            "tile_values": tile_values,
        })

    return scenarios


# ---------------------------------------------------------------------------
# 4. DRAW ONE MAP'S BEST-CASE RESULT INTO A GIVEN AXIS
# ---------------------------------------------------------------------------
def draw_map_panel(fig, ax, scenario: dict):
    resources = scenario["resources"]
    dice = scenario["dice"]
    tile_values = scenario["tile_values"]
    opt_values = scenario["opt_values"]
    best_town = scenario["best_town"]
    road_from, road_to = scenario["road_from"], scenario["road_to"]

    tile_cmap = plt.cm.YlOrRd
    town_cmap = plt.cm.GnBu

    for h in range(19):
        cx, cy = HEX_CENTERS[h]
        is_desert = resources[h] == 5
        face = "#e0e0e0" if is_desert else tile_cmap(tile_values[h])
        hexagon = RegularPolygon((cx, cy), numVertices=6, radius=HEX_SIZE, orientation=0,
                                  facecolor=face, edgecolor="black", linewidth=1.0, zorder=1)
        ax.add_patch(hexagon)

        r_name = RESOURCE_NAMES[resources[h]]
        d_num = str(dice[h]) if dice[h] != 0 else ""
        ax.text(cx, cy + 0.18, r_name, ha="center", va="center", fontsize=9,
                fontweight="bold", zorder=2)
        if d_num:
            ax.text(cx, cy - 0.22, d_num, ha="center", va="center", fontsize=11,
                    fontweight="bold", zorder=2)

    for a, b in PHYSICAL_EDGES:
        (x1, y1), (x2, y2) = TOWN_POSITIONS[a], TOWN_POSITIONS[b]
        ax.plot([x1, x2], [y1, y2], color="#555555", linewidth=1.0, zorder=3)

    vmin, vmax = min(opt_values), max(opt_values)
    for t in range(54):
        x, y = TOWN_POSITIONS[t]
        color = town_cmap((opt_values[t] - vmin) / (vmax - vmin) if vmax > vmin else 0)
        ax.scatter(x, y, s=270, color=color, edgecolors="black", linewidths=0.8, zorder=4)
        ax.text(x, y, str(t), ha="center", va="center", fontsize=7, zorder=5)

    bx, by = TOWN_POSITIONS[best_town]
    ax.scatter(bx, by, s=650, facecolors="none", edgecolors="gold", linewidths=3, zorder=6)
    ax.scatter(bx, by, s=45, marker="*", color="gold", edgecolors="black", linewidths=0.6, zorder=7)

    is_physical = True
    if road_to != -1:
        is_physical = tuple(sorted((road_from, road_to))) in PHYSICAL_EDGES
        arrow_color = "#1f77ff" if is_physical else "#e0218a"
        ax.annotate(
            "", xy=TOWN_POSITIONS[road_to], xytext=TOWN_POSITIONS[road_from],
            arrowprops=dict(arrowstyle="-|>", color=arrow_color, lw=3, mutation_scale=20,
                             shrinkA=9, shrinkB=9),
            zorder=8,
        )

    # per-panel legend: tile-ranking scale (0-1, same meaning on every map) on the left
    # of this panel, and this map's own town-score scale on the right (each map has a
    # different OptLocation range, so this one is normalized per panel, not shared)
    sm_tile = plt.cm.ScalarMappable(cmap=tile_cmap, norm=plt.Normalize(vmin=0, vmax=1))
    sm_tile.set_array([])
    cb1 = fig.colorbar(sm_tile, ax=ax, fraction=0.04, pad=0.10, location="left")
    cb1.set_label("Tile ranking", fontsize=8)
    cb1.ax.tick_params(labelsize=7)

    sm_town = plt.cm.ScalarMappable(cmap=town_cmap, norm=plt.Normalize(vmin=vmin, vmax=vmax))
    sm_town.set_array([])
    cb2 = fig.colorbar(sm_town, ax=ax, fraction=0.04, pad=0.10)
    cb2.set_label("Town Opt Location", fontsize=8)
    cb2.ax.tick_params(labelsize=7)

    title = (
        f"Map {scenario['num']} — {scenario['strategy']}\n"
        f"Best Town: {best_town}   |   Road: {road_from} -> {road_to}"
    )
    ax.set_title(title, fontsize=10, fontweight="bold", pad=10)

    ax.set_xlim(-5.2, 5.2)
    ax.set_ylim(-7.3, 1.5)
    ax.set_aspect("equal")
    ax.axis("off")


# ---------------------------------------------------------------------------
# 5. MAIN
# ---------------------------------------------------------------------------
def main():
    print("Compiling & running the Java road-placement test scenarios...")
    raw_output = run_java_harness()

    scenarios = parse_scenarios(raw_output)
    print(f"Parsed {len(scenarios)} scenario(s).")

    # scenarios 2, 3, 4 are the random reshuffled boards (scenario 1 is the fixed default board)
    chosen = [s for s in scenarios if s["num"] in (2, 3, 4)]
    for s in chosen:
        print(f"Map {s['num']}: strategy = {s['strategy']}, best town = {s['best_town']}, "
              f"road = {s['road_from']} -> {s['road_to']}")

    fig, axes = plt.subplots(1, 3, figsize=(36, 12))
    for ax, scenario in zip(axes, chosen):
        draw_map_panel(fig, ax, scenario)

    fig.suptitle("Best-case strategy per random map (auto-selected top strategy for each board)",
                 fontsize=15, fontweight="bold")
    fig.tight_layout(rect=[0, 0, 1, 0.96])
    fig.subplots_adjust(wspace=0.55)

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    out_path = OUTPUT_DIR / "best_case_multimap.png"
    fig.savefig(out_path, dpi=150)
    plt.close(fig)
    print(f"Saved {out_path}")


if __name__ == "__main__":
    main()
