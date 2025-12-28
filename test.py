import matplotlib.pyplot as plt
import networkx as nx
import numpy as np

# ==========================================
# 1. DATA INPUT
# ==========================================
data_str = """
0.5 0.5 0.75 1.0 0.75 0.5 1.0 1.0 0.75 0.0 0.75 0.75 0.5 0.25 1.0 0.5 0.5 0.25 1.0 3.0
0.5 0.25 0.75 0.75 1.0 1.0 0.75 0.5 0.75 0.0 0.75 0.25 0.25 0.25 0.5 1.0 0.25 0.0 0.5 4.0
0.5 1.0 0.75 0.5 0.25 0.5 0.5 1.0 0.5 0.0 0.75 1.0 1.0 0.25 0.75 0.5 1.0 0.75 1.0 1.0
0.0 0.25 0.5 0.75 1.0 1.0 0.75 0.5 0.75 0.0 0.0 0.25 0.25 0.0 0.5 1.0 0.25 0.0 0.5 2.0
0.0 0.5 0.5 1.0 0.75 0.5 1.0 1.0 0.25 0.0 0.0 0.75 0.5 0.0 0.5 0.5 0.5 0.25 1.0 0.0
"""

win_condition_names = [
    "4 Cities + LR",
    "2 Cities + 1 Settlement + LR & LA (Extra Brick/Wood)",
    "3 Cities + 2 Settlements + LR",
    "2 Cities + 3 Settlements + LR",
    "1 City + 3 Settlements + LR & LA"
]

# Map Attributes
map_resources = [2,4,2,0,0,3,4,3,1,5,2,1,4,2,1,3,4,0,3]
map_dice = [5,11,12,9,8,4,5,8,3,0,10,9,3,6,4,10,11,2,6]
resource_names = {0: 'Brick', 1: 'Ore', 2: 'Sheep', 3: 'Wheat', 4: 'Wood', 5: 'Desert'}

# Parse Data
heatmap_rows = []
row_ids = []
for line in data_str.strip().split('\n'):
    values = [float(x) for x in line.split()]
    heatmap_rows.append(values[:-1])  # First 19 items
    row_ids.append(int(values[-1]))   # Last item is the ID

# ==========================================
# 2. GRAPH TOPOLOGY
# ==========================================
G = nx.Graph()
G.add_nodes_from(range(19))
pos = {
    9: (0, 0), 8: (0, 2), 13: (1.73, 1), 14: (1.73, -1), 10: (0, -2), 5: (-1.73, -1), 4: (-1.73, 1),
    7: (0, 4), 12: (1.73, 3), 16: (3.46, 2), 17: (3.46, 0), 18: (3.46, -2), 15: (1.73, -3),
    11: (0, -4), 6: (-1.73, -3), 2: (-3.46, -2), 1: (-3.46, 0), 0: (-3.46, 2), 3: (-1.73, 3)
}
edges = [
    (9,4), (9,8), (9,13), (9,14), (9,10), (9,5), (4,8), (8,13), (13,14), (14,10), (10,5), (5,4),
    (4,0), (4,1), (4,3), (8,3), (8,7), (8,12), (13,12), (13,16), (13,17), (14,17), (14,18), (14,15),
    (10,15), (10,11), (10,6), (5,6), (5,2), (5,1), (0,3), (3,7), (7,12), (12,16), (16,17), (17,18),
    (18,15), (15,11), (11,6), (6,2), (2,1), (1,0)
]
G.add_edges_from(edges)

# ==========================================
# 3. PLOTTING FUNCTION
# ==========================================
def draw_map(ax, values, title):
    # Normalize (0 to 1) for consistent coloring relative to the max of that map
    max_val = max(values) if max(values) > 0 else 1
    norm_values = [v/max_val for v in values]
    
    # Colors & Labels
    node_colors = [plt.cm.YlOrRd(v) for v in norm_values]
    labels = {}
    for i in range(19):
        r_name = resource_names[map_resources[i]]
        d_num = str(map_dice[i]) if map_dice[i] != 0 else ""
        labels[i] = f"{r_name}\n{d_num}"
    
    # Draw
    nx.draw_networkx_edges(G, pos, ax=ax, edge_color='#E0E0E0', width=2)
    nx.draw_networkx_nodes(G, pos, ax=ax, node_color=node_colors, node_size=1800, edgecolors='black')
    nx.draw_networkx_labels(G, pos, labels, ax=ax, font_size=8, font_weight='bold')
    
    ax.set_title(title, fontsize=10, fontweight='bold')
    ax.axis('off')

# ==========================================
# 4. EXECUTE DRAWING
# ==========================================
# We have 5 strategies, so we keep 2x3 grid but hide the last one
fig, axes = plt.subplots(2, 3, figsize=(20, 12))
axes = axes.flatten()

# Plot the 5 strategies
for i in range(5):
    win_id = row_ids[i]
    name = win_condition_names[win_id] if 0 <= win_id < len(win_condition_names) else f"Unknown ID {win_id}"
    draw_map(axes[i], heatmap_rows[i], f"Strategy {i+1}: {name}")

# Hide the 6th slot (index 5) since we removed the combined overlay
axes[5].axis('off')

# ==========================================
# 5. ADD INTENSITY BAR (Colorbar)
# ==========================================
# Create a dummy ScalarMappable to generate the legend
sm = plt.cm.ScalarMappable(cmap=plt.cm.YlOrRd, norm=plt.Normalize(vmin=0, vmax=1))
sm.set_array([])

# Add the colorbar to the figure
cbar = fig.colorbar(sm, ax=axes.tolist(), orientation='vertical', fraction=0.02, pad=0.04)
cbar.set_label('Relative Importance (Intensity)', fontsize=14, labelpad=10)
cbar.ax.tick_params(labelsize=10)

plt.suptitle("Catan Strategy Heatmaps", fontsize=20, fontweight='bold')
plt.show()