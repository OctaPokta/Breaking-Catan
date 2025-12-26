import matplotlib.pyplot as plt
import networkx as nx
import numpy as np

# ==========================================
# 1. CONFIGURATION: MANUAL OVERLAY
# ==========================================
# Choose which rows to combine by their file line number (0-4)
active_rows = [4] 

# ==========================================
# 2. DATA INPUT
# ==========================================
# Your new data (20 columns: 19 tile values + 1 ID at the end)
data_str = """
1.0 1.0 0.75 0.25 0.75 0.75 0.5 0.5 0.5 1.0 1.0 0.0 1.0 0.5 1.0 1.0 0.0 0.25 1.0 3.0
0.75 0.5 0.5 0.0 0.5 0.75 0.25 0.0 0.5 1.0 0.5 0.5 1.0 1.0 0.75 0.75 0.0 0.25 0.5 4.0
0.5 1.0 0.75 0.75 0.75 0.5 1.0 0.75 0.5 1.0 1.0 0.0 1.0 0.5 0.5 0.5 0.0 0.25 0.75 1.0
1.0 1.0 0.75 0.25 0.75 0.25 0.5 1.0 0.0 0.25 1.0 0.0 0.25 0.5 1.0 1.0 0.0 0.0 0.5 0.0
0.75 0.5 0.5 0.0 0.5 0.75 0.25 0.0 0.0 0.25 0.5 0.5 0.25 1.0 0.75 0.75 0.0 0.0 0.5 2.0
"""

# The names corresponding to the indices found at the end of each data row
win_condition_names = [
    "4 Cities + LR",
    "2 Cities + 1 Settlement + LR & LA (Extra Brick & Wood)",
    "3 cities + 2 settlements + LR",
    "2 Cities + 3 Settlements + LR",
    "1 City + 3 Settlements + LR & LA"                    
]

# Map Attributes
map_resources = [4, 3, 4, 0, 4, 1, 0, 1, 2, 2, 3, 3, 2, 3, 0, 4, 5, 2, 1]
map_dice = [5, 8, 4, 2, 10, 3, 11, 6, 9, 11, 6, 12, 3, 4, 5, 9, 0, 8, 10]
resource_names = {0: 'Brick', 1: 'Ore', 2: 'Sheep', 3: 'Wheat', 4: 'Wood', 5: 'Desert'}

# Parse Data
# We separate the first 19 values (heatmap) from the last value (win condition ID)
heatmap_rows = []
row_ids = []

for line in data_str.strip().split('\n'):
    values = [float(x) for x in line.split()]
    heatmap_rows.append(values[:-1])  # First 19 items
    row_ids.append(int(values[-1]))   # Last item is the ID

# ==========================================
# 3. CALCULATE HEATMAP
# ==========================================
selected_data = [heatmap_rows[i] for i in active_rows]

# Calculate Sum
if not selected_data:
    final_values = np.zeros(19)
else:
    final_values = np.sum(selected_data, axis=0)

# Normalize (0 to 1)
max_val = np.max(final_values)
if max_val == 0: max_val = 1
norm_values = final_values / max_val

# Determine Title Name
# If one row is selected, show its specific name. If multiple, show "Combined".
if len(active_rows) == 1:
    idx = active_rows[0]
    win_id = row_ids[idx]
    # Safety check for index out of range
    if 0 <= win_id < len(win_condition_names):
        display_title = win_condition_names[win_id]
    else:
        display_title = f"Unknown Condition (ID {win_id})"
else:
    display_title = "Combined Strategies Overlay"

# ==========================================
# 4. DRAWING
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

fig, ax = plt.subplots(figsize=(12, 12))

cmap = plt.cm.coolwarm 
node_colors = [cmap(v) for v in norm_values]

labels = {}
for i in range(19):
    r_name = resource_names[map_resources[i]]
    d_num = str(map_dice[i]) if map_dice[i] != 0 else ""
    labels[i] = f"{r_name}\n{d_num}"

nx.draw_networkx_edges(G, pos, ax=ax, edge_color='#E0E0E0', width=2)
nx.draw_networkx_nodes(G, pos, ax=ax, node_color=node_colors, node_size=3000, edgecolors='black')
nx.draw_networkx_labels(G, pos, labels, ax=ax, font_size=9, font_weight='bold', font_color='black')

sm = plt.cm.ScalarMappable(cmap=cmap, norm=plt.Normalize(vmin=0, vmax=max_val))
sm.set_array([])
cbar = plt.colorbar(sm, ax=ax, shrink=0.7)
cbar.set_label('Heatmap Value', fontsize=12)

# Dynamic Title
plt.title(f"Strategy: {display_title}", fontsize=16, fontweight='bold', pad=20)
plt.axis('off')
plt.show()