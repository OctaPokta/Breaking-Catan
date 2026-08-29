"""
Visualizes the FULL Dijkstra path (not just the first-step road direction) that
Road.java found for one scenario from RoadTestScenarios - by default, Scenario 4
(the rightmost of the 3 random maps).

Usage: python visualize_dijkstra_path.py [scenario_number]
Requires: a JDK on PATH (javac/java), matplotlib.
"""

import math
import re
import subprocess
import sys
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import RegularPolygon

PROJECT_ROOT = Path(__file__).resolve().parents[1]
SOURCE_ROOT = PROJECT_ROOT.parent
BUILD_DIR = PROJECT_ROOT / "testing" / "_build"
OUTPUT_DIR = PROJECT_ROOT / "testing" / "_dijkstra_path_plots"

RESOURCE_NAMES = {0: "Brick", 1: "Ore", 2: "Sheep", 3: "Wheat", 4: "Wood", 5: "Desert"}

HEX_TO_TOWNS_MAP = [
    [0,3,4,7,8,12],[1,4,5,8,9,13],[2,5,6,9,10,14],
    [7,11,12,16,17,22],[8,12,13,17,18,23],[9,13,14,18,19,24],[10,14,15,19,20,25],
    [16,21,22,27,28,33],[17,22,23,28,29,34],[18,23,24,29,30,35],[19,24,25,30,31,36],[20,25,26,31,32,37],
    [28,33,34,38,39,43],[29,34,35,39,40,44],[30,35,36,40,41,45],[31,36,37,41,42,46],
    [39,43,44,47,48,51],[40,44,45,48,49,52],[41,45,46,49,50,53],
]

def _build_hex_centers():
    rows = [[0,1,2],[3,4,5,6],[7,8,9,10,11],[12,13,14,15],[16,17,18]]
    size = 1.0; horiz = math.sqrt(3)*size; vert = 1.5*size
    centers = {}
    for r, row in enumerate(rows):
        L = len(row)
        for i, h in enumerate(row):
            centers[h] = ((i - (L-1)/2)*horiz, -r*vert)
    return centers

HEX_CENTERS = _build_hex_centers()
HEX_SIZE = 1.0

def _hex_corner_list(cx, cy):
    return [(round(cx + HEX_SIZE*math.cos(math.radians(60*i-30)), 4),
             round(cy + HEX_SIZE*math.sin(math.radians(60*i-30)), 4)) for i in range(6)]

HEX_CORNERS = {h: _hex_corner_list(*HEX_CENTERS[h]) for h in range(19)}

def _solve_town_positions():
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
            for h in hexes: claimed[h].add(pos)
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
                    for h in hexes: claimed[h].add(free[0])
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
        if not free_towns: continue
        cx, cy = HEX_CENTERS[h]
        free_corners.sort(key=lambda c: math.atan2(c[1]-cy, c[0]-cx))
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
            a, b = HEX_CORNERS[h][i], HEX_CORNERS[h][(i+1) % 6]
            if a in corner_to_town and b in corner_to_town:
                edges.add(tuple(sorted((corner_to_town[a], corner_to_town[b]))))
    return edges

PHYSICAL_EDGES = _solve_physical_edges()


def run_java_harness() -> str:
    BUILD_DIR.mkdir(parents=True, exist_ok=True)
    java_files = [str(p) for p in SOURCE_ROOT.glob("CatanAnalysis/**/*.java")]
    result = subprocess.run(["javac", "-d", str(BUILD_DIR)] + java_files, capture_output=True, text=True)
    if result.returncode != 0:
        print(result.stdout); print(result.stderr)
        sys.exit("Java compilation failed.")
    result = subprocess.run(["java", "-cp", str(BUILD_DIR), "CatanAnalysis.testing.RoadTestScenarios"],
                             capture_output=True, text=True)
    if result.returncode != 0:
        print(result.stdout); print(result.stderr)
        sys.exit("Running RoadTestScenarios failed.")
    return result.stdout


def parse_scenario(raw_output: str, scenario_num: int):
    blocks = re.split(r"=== SCENARIO (\d+) ===", raw_output)[1:]
    for i in range(0, len(blocks), 2):
        if int(blocks[i]) != scenario_num:
            continue
        body = blocks[i + 1]
        return {
            "num": scenario_num,
            "strategy": re.search(r"STRATEGY:\s*(.+)", body).group(1).strip(),
            "resources": [int(x) for x in re.search(r"RESOURCES:\s*(.+)", body).group(1).split()],
            "dice": [int(x) for x in re.search(r"DICE:\s*(.+)", body).group(1).split()],
            "best_town": int(re.search(r"BEST_TOWN:\s*(\d+)", body).group(1)),
            "full_path": [int(x) for x in re.search(r"FULL_PATH:\s*(.+)", body).group(1).split()],
            "path_cost": float(re.search(r"PATH_TARGET_COST:\s*([\d.]+)", body).group(1)),
            "opt_values": [float(x) for x in re.search(r"OPT_VALUES:\s*(.+)", body).group(1).split()],
            "tile_values": [float(x) for x in re.search(r"TILE_VALUES:\s*(.+)", body).group(1).split()],
        }
    sys.exit(f"Scenario {scenario_num} not found in output.")


def render(scenario: dict, out_path: Path):
    fig, ax = plt.subplots(figsize=(12, 13))

    resources, dice = scenario["resources"], scenario["dice"]
    tile_values, opt_values = scenario["tile_values"], scenario["opt_values"]
    path = scenario["full_path"]

    tile_cmap = plt.cm.YlOrRd
    town_cmap = plt.cm.GnBu

    for h in range(19):
        cx, cy = HEX_CENTERS[h]
        is_desert = resources[h] == 5
        face = "#e0e0e0" if is_desert else tile_cmap(tile_values[h])
        ax.add_patch(RegularPolygon((cx, cy), numVertices=6, radius=HEX_SIZE, orientation=0,
                                     facecolor=face, edgecolor="black", linewidth=1.2, zorder=1))
        r_name = RESOURCE_NAMES[resources[h]]
        d_num = str(dice[h]) if dice[h] != 0 else ""
        ax.text(cx, cy + 0.18, r_name, ha="center", va="center", fontsize=9, fontweight="bold", zorder=2)
        if d_num:
            ax.text(cx, cy - 0.22, d_num, ha="center", va="center", fontsize=11, fontweight="bold", zorder=2)

    for a, b in PHYSICAL_EDGES:
        (x1, y1), (x2, y2) = TOWN_POSITIONS[a], TOWN_POSITIONS[b]
        ax.plot([x1, x2], [y1, y2], color="#555555", linewidth=1.3, zorder=3)

    vmin, vmax = min(opt_values), max(opt_values)
    for t in range(54):
        x, y = TOWN_POSITIONS[t]
        color = town_cmap((opt_values[t] - vmin) / (vmax - vmin) if vmax > vmin else 0)
        ax.scatter(x, y, s=270, color=color, edgecolors="black", linewidths=0.8, zorder=4)
        ax.text(x, y, str(t), ha="center", va="center", fontsize=7, zorder=5)

    # highlight the FULL dijkstra path as a sequence of bold arrows
    for i in range(len(path) - 1):
        a, b = path[i], path[i + 1]
        ax.annotate(
            "", xy=TOWN_POSITIONS[b], xytext=TOWN_POSITIONS[a],
            arrowprops=dict(arrowstyle="-|>", color="#1f77ff", lw=4, mutation_scale=24,
                             shrinkA=11, shrinkB=11),
            zorder=8,
        )

    # start + target markers
    start, target = path[0], path[-1]
    sx, sy = TOWN_POSITIONS[start]
    ax.scatter(sx, sy, s=750, facecolors="none", edgecolors="gold", linewidths=3.5, zorder=6)
    ax.scatter(sx, sy, s=55, marker="*", color="gold", edgecolors="black", linewidths=0.7, zorder=7)

    if target != start:
        tx, ty = TOWN_POSITIONS[target]
        ax.scatter(tx, ty, s=750, facecolors="none", edgecolors="#1f77ff", linewidths=3.5, zorder=6)
        ax.scatter(tx, ty, s=55, marker="D", color="#1f77ff", edgecolors="black", linewidths=0.7, zorder=7)

    sm_tile = plt.cm.ScalarMappable(cmap=tile_cmap, norm=plt.Normalize(vmin=0, vmax=1))
    sm_tile.set_array([])
    cb1 = fig.colorbar(sm_tile, ax=ax, fraction=0.045, pad=0.03, location="left")
    cb1.set_label("Tile ranking", fontsize=9)

    sm_town = plt.cm.ScalarMappable(cmap=town_cmap, norm=plt.Normalize(vmin=vmin, vmax=vmax))
    sm_town.set_array([])
    cb2 = fig.colorbar(sm_town, ax=ax, fraction=0.045, pad=0.03)
    cb2.set_label("Town Opt Location", fontsize=9)

    hops = len(path) - 1
    path_str = " -> ".join(str(t) for t in path)
    title = (
        f"Map {scenario['num']} — Strategy: {scenario['strategy']}\n"
        f"Full Dijkstra path ({hops} hop{'s' if hops != 1 else ''}, cumulative cost={scenario['path_cost']:.3f}): {path_str}\n"
        f"gold star = starting town {start}   |   blue diamond = Dijkstra's overall best target {target}"
    )
    ax.set_title(title, fontsize=11, fontweight="bold", pad=14)

    ax.set_xlim(-5.2, 5.2)
    ax.set_ylim(-7.3, 1.5)
    ax.set_aspect("equal")
    ax.axis("off")

    fig.tight_layout()
    fig.savefig(out_path, dpi=150)
    plt.close(fig)


def main():
    scenario_num = int(sys.argv[1]) if len(sys.argv) > 1 else 4
    print(f"Compiling & running RoadTestScenarios, extracting scenario {scenario_num}...")
    raw_output = run_java_harness()
    scenario = parse_scenario(raw_output, scenario_num)

    print(f"Strategy: {scenario['strategy']}")
    print(f"Best town: {scenario['best_town']}")
    print(f"Full Dijkstra path: {' -> '.join(str(t) for t in scenario['full_path'])}")
    print(f"Cumulative cost to target: {scenario['path_cost']:.3f}")

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    out_path = OUTPUT_DIR / f"dijkstra_path_scenario_{scenario_num}.png"
    render(scenario, out_path)
    print(f"Saved {out_path}")


if __name__ == "__main__":
    main()
