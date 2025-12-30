CatanAnalysis is a specialized tool designed to determine the mathematically optimal starting positions on any given Settlers of Catan map layout. By leveraging graph theory and custom heat-mapping algorithms, the project simulates various "Win Strategies" to identify high-value territories for settlement and expansion.

Overview

The project treats the Catan board as a complex graph where tiles (hexagons) are nodes. It evaluates the board based on resource probability (dice numbers) and strategic importance, ultimately clustering the map into "Connected Components" to find the best areas for long-term growth.
Key Features

    Strategic Win-Condition Modeling: Analyzes the board based on different user-defined winning strategies.

    Geoverse Mapping Integration: Utilizes a custom importance index (1.0, 0.75, 0.5, 0.25) to rank the "Relative Importance" of every tile on the board.

    Graph Clustering: Uses Depth-First Search (DFS) to partition the board into Connected Components, identifying contiguous high-value regions.

    Probabilistic Heatmaps: Generates resource probability visualizations based on dice rolls and resource types (e.g., Brick, Wood, Wheat, etc.).

    Data-Driven Ranking: Automatically sorts potential settlement areas by size and strategic value to recommend the best starting hexes.

Technical Stack

    Language: Java (utilizing Graph Theory and custom Data Structures).

    Core Algorithms: * DFS (Depth-First Search): For identifying connected components in the adjacency matrix.

        Custom Sorting: Hierarchical ranking of components based on size and importance.

    Data Architecture: * Adjacency Matrix representation of the hexagonal grid.

        Nested ArrayList structures for storing ConnectedComponentTile metadata.

How It Works

    Map Input: The user inputs the board configuration (resource types and dice numbers).

    Importance Mapping: Each tile is assigned a weight based on the probability of its number being rolled.

    Connectivity Analysis: * The system builds an Adjacency Matrix.

        It identifies "equivalence classes" of importance.

        It runs a DFS to find clusters of adjacent tiles within the same importance level.

    Optimal Placement: The algorithm outputs a ranked list of the largest and most valuable clusters, suggesting where to place initial settlements to maximize expansion potential.

Project Structure
