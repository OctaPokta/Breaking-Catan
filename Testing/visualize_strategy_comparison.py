"""
Runs the Java StrategyComparisonScenario harness (ALL 5 strategies against ONE fixed
map, ranked best -> worst for that map) and renders one fused image per strategy:
  - the hex board colored by that strategy's own pre-town tile-ranking heatmap
  - the town graph (colored by that strategy's OptLocation) fused at each town's real
    geometric position
  - the chosen BEST TOWN highlighted with a gold star
  - the chosen FIRST ROAD drawn as an arrow

Images are numbered by RANK (1 = best strategy for this map, 5 = worst), which is the
order the article presents results in: for the fixed board, rank 1 was
"2 Cities + 3 Settlements + LR" (original index 3), then "1 City + 3 Settlements + LR & LA"
(4), "2 Cities + 1 Settlement + LR & LA" (1), "3 cities + 2 settlements + LR" (2), and
"4 Cities + LR" (0) last.

Usage: python visualize_strategy_comparison.py
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
OUTPUT_DIR = PROJECT_ROOT / "testing" / "_strategy_comparison_plots"

RESOURCE_NAMES = {0: "Brick", 1: "Ore", 2: "Sheep", 3: "Wheat", 4: "Wood", 5: "Desert"}

# hexToTownsMap copied from Towns.java - for each of the 19 hexes, the 6 town indices
# that touch it (set membership only, order not meaningful - see note below)
HEX_TO_TOWNS_MAP = [
    [0,3,4,7,8,12],[1,4,5,8,9,13],[2,5,6,9,10,14],
    [7,11,12,16,17,22],[8,12,13,17,18,23],[9,13,14,18,19,24],[10,14,15,19,20,25],
    [16,21,22,27,28,33],[17,22,23,28,29,34],[18,23,24,29,30,35],[19,24,25,30,31,36],[20,25,26,31,32,37],
    [28,33,34,38,39,43],[29,34,35,39,40,44],[30,35,36,40,41,45],[31,36,37,41,42,46],
    [39,43,44,47,48,51],[40,44,45,48,49,52],[41,45,46,49,50,53],
]

# 19-hex board centers (rows of 3,4,5,4,3), pointy-top hex layout, hex index -> (x, y)
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
    HEX_TO_TOWNS_MAP's set-membership data - see visualize_road_scenarios.py for the
    full explanation of why (RoadConnectionGraph's matrix uses the same numbering
    since that file was regenerated from this same geometry)."""
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

    run_cmd = ["java", "-cp", str(BUILD_DIR), "CatanAnalysis.testing.StrategyComparisonScenario"]
    result = subprocess.run(run_cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(result.stdout)
        print(result.stderr)
        sys.exit("Running StrategyComparisonScenario failed - see errors above.")

    return result.stdout


# ---------------------------------------------------------------------------
# 3. PARSE THE STRUCTURED OUTPUT
# ---------------------------------------------------------------------------
def parse_output(raw_output: str):
    resources = [int(x) for x in re.search(r"RESOURCES:\s*(.+)", raw_output).group(1).split()]
    dice = [int(x) for x in re.search(r"DICE:\s*(.+)", raw_output).group(1).split()]

    ranks = []
    blocks = re.split(r"=== RANK (\d+) \(original strategy index (\d+)\) ===", raw_output)[1:]

    for i in range(0, len(blocks), 3):
        rank = int(blocks[i])
        original_index = int(blocks[i + 1])
        body = blocks[i + 2]

        strategy = re.search(r"STRATEGY:\s*(.+)", body).group(1).strip()
        best_town = int(re.search(r"BEST_TOWN:\s*(\d+)", body).group(1))
        road_match = re.search(r"ROAD:\s*(\d+)\s*->\s*(-?\d+)", body)
        road_from, road_to = int(road_match.group(1)), int(road_match.group(2))
        opt_values = [float(x) for x in re.search(r"OPT_VALUES:\s*(.+)", body).group(1).split()]
        tile_values = [float(x) for x in re.search(r"TILE_VALUES:\s*(.+)", body).group(1).split()]

        ranks.append({
            "rank": rank,
            "original_index": original_index,
            "strategy": strategy,
            "best_town": best_town,
            "road_from": road_from,
            "road_to": road_to,
            "opt_values": opt_values,
            "tile_values": tile_values,
        })

    return resources, dice, ranks


# ---------------------------------------------------------------------------
# 4. RENDER ONE STRATEGY'S RESULT (same board every time, different scoring)
# ---------------------------------------------------------------------------
def render_strategy(resources, dice, entry: dict, out_path: Path):
    fig, ax = plt.subplots(figsize=(11, 12))

    tile_values = entry["tile_values"]
    opt_values = entry["opt_values"]
    best_town = entry["best_town"]
    road_from, road_to = entry["road_from"], entry["road_to"]

    tile_cmap = plt.cm.YlOrRd
    town_cmap = plt.cm.GnBu

    for h in range(19):
        cx, cy = HEX_CENTERS[h]
        is_desert = resources[h] == 5
        face = "#e0e0e0" if is_desert else tile_cmap(tile_values[h])
        hexagon = RegularPolygon((cx, cy), numVertices=6, radius=HEX_SIZE, orientation=0,
                                  facecolor=face, edgecolor="black", linewidth=1.2, zorder=1)
        ax.add_patch(hexagon)

        r_name = RESOURCE_NAMES[resources[h]]
        d_num = str(dice[h]) if dice[h] != 0 else ""
        ax.text(cx, cy + 0.18, r_name, ha="center", va="center", fontsize=8.5,
                fontweight="bold", zorder=2)
        if d_num:
            ax.text(cx, cy - 0.22, d_num, ha="center", va="center", fontsize=10.5,
                    fontweight="bold", zorder=2)

    for a, b in PHYSICAL_EDGES:
        (x1, y1), (x2, y2) = TOWN_POSITIONS[a], TOWN_POSITIONS[b]
        ax.plot([x1, x2], [y1, y2], color="#555555", linewidth=1.3, zorder=3)

    vmin, vmax = min(opt_values), max(opt_values)
    for t in range(54):
        x, y = TOWN_POSITIONS[t]
        color = town_cmap((opt_values[t] - vmin) / (vmax - vmin) if vmax > vmin else 0)
        ax.scatter(x, y, s=230, color=color, edgecolors="black", linewidths=0.8, zorder=4)
        ax.text(x, y, str(t), ha="center", va="center", fontsize=5.5, zorder=5)

    bx, by = TOWN_POSITIONS[best_town]
    ax.scatter(bx, by, s=650, facecolors="none", edgecolors="gold", linewidths=3, zorder=6)
    ax.scatter(bx, by, s=45, marker="*", color="gold", edgecolors="black", linewidths=0.6, zorder=7)

    is_physical = True
    if road_to != -1:
        is_physical = tuple(sorted((road_from, road_to))) in PHYSICAL_EDGES
        arrow_color = "#1f77ff" if is_physical else "#e0218a"
        ax.annotate(
            "", xy=TOWN_POSITIONS[road_to], xytext=TOWN_POSITIONS[road_from],
            arrowprops=dict(arrowstyle="-|>", color=arrow_color, lw=3, mutation_scale=22,
                             shrinkA=10, shrinkB=10),
            zorder=8,
        )
        if not is_physical:
            mx, my = (TOWN_POSITIONS[road_from][0] + TOWN_POSITIONS[road_to][0]) / 2, \
                     (TOWN_POSITIONS[road_from][1] + TOWN_POSITIONS[road_to][1]) / 2
            ax.text(mx, my, "not a physical edge*", color=arrow_color, fontsize=7,
                    ha="center", va="center", fontweight="bold", zorder=9,
                    bbox=dict(facecolor="white", edgecolor="none", alpha=0.75, pad=1))

    sm_tile = plt.cm.ScalarMappable(cmap=tile_cmap, norm=plt.Normalize(vmin=0, vmax=1))
    sm_tile.set_array([])
    cb1 = fig.colorbar(sm_tile, ax=ax, fraction=0.04, pad=0.03, location="left")
    cb1.set_label("Tile ranking (pre-town heatmap)", fontsize=9)

    sm_town = plt.cm.ScalarMappable(cmap=town_cmap, norm=plt.Normalize(vmin=vmin, vmax=vmax))
    sm_town.set_array([])
    cb2 = fig.colorbar(sm_town, ax=ax, fraction=0.04, pad=0.03)
    cb2.set_label("Town Final Opt Location", fontsize=9)

    title = (
        f"Rank {entry['rank'] + 1} of 5 for this map — Strategy: {entry['strategy']}\n"
        f"Best Town: {best_town}   |   First Road: {road_from} -> {road_to}"
    )
    if road_to != -1 and not is_physical:
        title += "   (*not adjacent in the real board graph)"
    ax.set_title(title, fontsize=12, fontweight="bold", pad=14)

    ax.set_xlim(-5.2, 5.2)
    ax.set_ylim(-7.3, 1.5)
    ax.set_aspect("equal")
    ax.axis("off")

    fig.tight_layout()
    fig.savefig(out_path, dpi=150)
    plt.close(fig)


# ---------------------------------------------------------------------------
# 5. MAIN
# ---------------------------------------------------------------------------
def main():
    print("Compiling & running the strategy-comparison harness (one map, all 5 strategies)...")
    raw_output = run_java_harness()
    print(raw_output)

    resources, dice, ranks = parse_output(raw_output)
    print(f"Parsed {len(ranks)} strategies for this map, ranked best -> worst:")
    for entry in ranks:
        print(f"  rank {entry['rank'] + 1}: {entry['strategy']}  "
              f"(best town = {entry['best_town']}, road = {entry['road_from']} -> {entry['road_to']})")

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    for entry in ranks:
        out_path = OUTPUT_DIR / f"rank_{entry['rank'] + 1}_strategy_{entry['original_index']}.png"
        render_strategy(resources, dice, entry, out_path)
        print(f"Saved {out_path}")


if __name__ == "__main__":
    main()
