package CatanAnalysis.secondsettlement.gui;

import CatanAnalysis.secondsettlement.BoardGeometry;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JPanel;

/**
 * Interactive Catan board: draws the 19 hexes (resource + dice number) and overlays the
 * 54 town nodes and 72 road edges. Clicking a hex, town, or edge notifies a listener - what
 * the click actually DOES (edit resource, mark our/enemy settlement, mark our/enemy road,
 * erase) is decided by whichever "tool" is currently selected in CatanBoardApp.
 */
public class BoardPanel extends JPanel {

    public interface Listener {
        void onHexClicked(int hexIndex);
        void onTownClicked(int townIndex);
        void onEdgeClicked(int[] edge);
    }

    // resource type -> fill color (0=brick,1=ore,2=sheep,3=wheat,4=wood,5=desert)
    private static final Color[] RESOURCE_COLORS = {
        new Color(0xC96A3A), // brick
        new Color(0x8C8C99), // ore
        new Color(0x9ACD5C), // sheep
        new Color(0xE8C443), // wheat
        new Color(0x2F7A3C), // wood
        new Color(0xD8C48A)  // desert
    };

    public enum TownMark { NONE, OURS, ENEMY }
    public enum RoadMark { NONE, OURS, ENEMY }

    private final int[] hexResources = new int[BoardGeometry.TOTAL_HEXES];
    private final int[] hexDice = new int[BoardGeometry.TOTAL_HEXES];
    private final TownMark[] townMarks = new TownMark[BoardGeometry.TOTAL_TOWNS];
    private final Set<String> enemyRoadKeys = new HashSet<>();
    private final Set<String> ourRoadKeys = new HashSet<>();

    // optional result overlay
    private double[] townHeatValues = null; // 54, null = no heatmap
    private double[] tileHeatValues = null; // 19, null = no heatmap
    private int recommendedTown = -1;
    private int[] recommendedPath = new int[0];

    private Listener listener;

    private double scale = 46;
    private int originX, originY;
    private final HashMap<Integer, java.awt.Point> hexScreenPos = new HashMap<>();
    private final HashMap<Integer, java.awt.Point> townScreenPos = new HashMap<>();

    public BoardPanel() {
        setBackground(new Color(0x1B3A5C));
        for (int i = 0; i < townMarks.length; i++) townMarks[i] = TownMark.NONE;
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick(e.getX(), e.getY());
            }
        });
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    // ---- state getters/setters used by CatanBoardApp ----

    public int getHexResource(int hex) { return hexResources[hex]; }
    public int getHexDice(int hex) { return hexDice[hex]; }

    public void setHex(int hex, int resource, int dice) {
        hexResources[hex] = resource;
        hexDice[hex] = dice;
        repaint();
    }

    public int[] getAllResources() { return hexResources.clone(); }
    public int[] getAllDice() { return hexDice.clone(); }

    public TownMark getTownMark(int town) { return townMarks[town]; }

    public void setTownMark(int town, TownMark mark) {
        townMarks[town] = mark;
        repaint();
    }

    public java.util.List<Integer> getTownsMarked(TownMark mark) {
        java.util.List<Integer> result = new java.util.ArrayList<>();
        for (int i = 0; i < townMarks.length; i++) {
            if (townMarks[i] == mark) result.add(i);
        }
        return result;
    }

    private static String edgeKey(int a, int b) {
        return Math.min(a, b) + "-" + Math.max(a, b);
    }

    public RoadMark getRoadMark(int a, int b) {
        String key = edgeKey(a, b);
        if (ourRoadKeys.contains(key)) return RoadMark.OURS;
        if (enemyRoadKeys.contains(key)) return RoadMark.ENEMY;
        return RoadMark.NONE;
    }

    public void setRoadMark(int a, int b, RoadMark mark) {
        String key = edgeKey(a, b);
        ourRoadKeys.remove(key);
        enemyRoadKeys.remove(key);
        if (mark == RoadMark.OURS) ourRoadKeys.add(key);
        else if (mark == RoadMark.ENEMY) enemyRoadKeys.add(key);
        repaint();
    }

    public java.util.List<int[]> getEnemyRoadEdges() {
        java.util.List<int[]> result = new java.util.ArrayList<>();
        for (String key : enemyRoadKeys) {
            String[] parts = key.split("-");
            result.add(new int[]{ Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) });
        }
        return result;
    }

    public void setResultOverlay(double[] townHeatValues, double[] tileHeatValues, int recommendedTown, int[] recommendedPath) {
        this.townHeatValues = townHeatValues;
        this.tileHeatValues = tileHeatValues;
        this.recommendedTown = recommendedTown;
        this.recommendedPath = recommendedPath != null ? recommendedPath : new int[0];
        repaint();
    }

    public void clearResultOverlay() {
        setResultOverlay(null, null, -1, new int[0]);
    }

    // ---- layout ----

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        computeLayout();
        drawHexes(g);
        drawEdges(g);
        drawTowns(g);
    }

    private void computeLayout() {
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int h = 0; h < BoardGeometry.TOTAL_HEXES; h++) {
            minX = Math.min(minX, BoardGeometry.HEX_X[h]);
            maxX = Math.max(maxX, BoardGeometry.HEX_X[h]);
            minY = Math.min(minY, BoardGeometry.HEX_Y[h]);
            maxY = Math.max(maxY, BoardGeometry.HEX_Y[h]);
        }
        double modelW = (maxX - minX) + 4; // + hex width margin
        double modelH = (maxY - minY) + 4;
        double sx = getWidth() / modelW;
        double sy = getHeight() / modelH;
        scale = Math.max(20, Math.min(sx, sy) * 0.95);

        originX = getWidth() / 2;
        originY = getHeight() / 2;
        double midX = (minX + maxX) / 2;
        double midY = (minY + maxY) / 2;

        hexScreenPos.clear();
        for (int h = 0; h < BoardGeometry.TOTAL_HEXES; h++) {
            int x = originX + (int) Math.round((BoardGeometry.HEX_X[h] - midX) * scale);
            int y = originY + (int) Math.round((BoardGeometry.HEX_Y[h] - midY) * scale);
            hexScreenPos.put(h, new java.awt.Point(x, y));
        }
        townScreenPos.clear();
        for (int t = 0; t < BoardGeometry.TOTAL_TOWNS; t++) {
            int x = originX + (int) Math.round((BoardGeometry.TOWN_X[t] - midX) * scale);
            int y = originY + (int) Math.round((BoardGeometry.TOWN_Y[t] - midY) * scale);
            townScreenPos.put(t, new java.awt.Point(x, y));
        }
    }

    private java.awt.Point hexCorner(int hex, int corner) {
        java.awt.Point c = hexScreenPos.get(hex);
        double angle = Math.toRadians(60 * corner - 30);
        int x = c.x + (int) Math.round(scale * Math.cos(angle));
        int y = c.y + (int) Math.round(scale * Math.sin(angle));
        return new java.awt.Point(x, y);
    }

    private Color heatColor(double value) {
        // simple YlOrRd-ish ramp: value in [0,1] -> pale yellow to deep red
        value = Math.max(0, Math.min(1, value));
        int r = 255;
        int gr = (int) (255 - value * 170);
        int b = (int) (170 - value * 170);
        return new Color(r, Math.max(0, gr), Math.max(0, b));
    }

    private void drawHexes(Graphics2D g) {
        for (int h = 0; h < BoardGeometry.TOTAL_HEXES; h++) {
            java.awt.Point[] pts = new java.awt.Point[6];
            for (int c = 0; c < 6; c++) pts[c] = hexCorner(h, c);
            int[] xs = new int[6], ys = new int[6];
            for (int c = 0; c < 6; c++) { xs[c] = pts[c].x; ys[c] = pts[c].y; }

            Color fill;
            if (tileHeatValues != null) {
                fill = heatColor(tileHeatValues[h]);
            } else {
                int resource = hexResources[h];
                fill = RESOURCE_COLORS[Math.max(0, Math.min(5, resource))];
            }
            g.setColor(fill);
            g.fillPolygon(xs, ys, 6);
            g.setColor(new Color(0x102640));
            g.setStroke(new BasicStroke(2f));
            g.drawPolygon(xs, ys, 6);

            java.awt.Point center = hexScreenPos.get(h);
            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            String resName = BoardGeometry.RESOURCE_NAMES[Math.max(0, Math.min(5, hexResources[h]))];
            drawCentered(g, resName, center.x, center.y - 4);
            String diceText = hexDice[h] == 0 ? "-" : String.valueOf(hexDice[h]);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            drawCentered(g, diceText, center.x, center.y + 14);
        }
    }

    private void drawEdges(Graphics2D g) {
        for (int[] edge : BoardGeometry.EDGES) {
            java.awt.Point a = townScreenPos.get(edge[0]);
            java.awt.Point b = townScreenPos.get(edge[1]);
            RoadMark mark = getRoadMark(edge[0], edge[1]);

            boolean onPath = isOnRecommendedPath(edge[0], edge[1]);

            if (mark == RoadMark.OURS) {
                g.setColor(new Color(0x2E8B57));
                g.setStroke(new BasicStroke(6f));
            } else if (mark == RoadMark.ENEMY) {
                g.setColor(new Color(0xB03A2E));
                g.setStroke(new BasicStroke(6f));
            } else if (onPath) {
                g.setColor(new Color(0xFFD700));
                g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{2f, 6f}, 0f));
            } else {
                g.setColor(new Color(0xAEB6BF));
                g.setStroke(new BasicStroke(2f));
            }
            g.drawLine(a.x, a.y, b.x, b.y);
        }
    }

    private boolean isOnRecommendedPath(int a, int b) {
        for (int i = 0; i + 1 < recommendedPath.length; i++) {
            if ((recommendedPath[i] == a && recommendedPath[i + 1] == b) ||
                (recommendedPath[i] == b && recommendedPath[i + 1] == a)) {
                return true;
            }
        }
        return false;
    }

    private void drawTowns(Graphics2D g) {
        for (int t = 0; t < BoardGeometry.TOTAL_TOWNS; t++) {
            java.awt.Point p = townScreenPos.get(t);
            double radius = 9;

            Color fill;
            if (t == recommendedTown) {
                fill = new Color(0xFFD700);
            } else if (townMarks[t] == TownMark.OURS) {
                fill = new Color(0x2E8B57);
            } else if (townMarks[t] == TownMark.ENEMY) {
                fill = new Color(0xB03A2E);
            } else if (townHeatValues != null) {
                double max = 0;
                for (double v : townHeatValues) max = Math.max(max, v);
                double norm = max > 0 ? townHeatValues[t] / max : 0;
                fill = heatColor(norm);
            } else {
                fill = new Color(0xEFEFEF);
            }

            Ellipse2D circle = new Ellipse2D.Double(p.x - radius, p.y - radius, radius * 2, radius * 2);
            g.setColor(fill);
            g.fill(circle);
            g.setColor(t == recommendedTown ? Color.BLACK : new Color(0x102640));
            g.setStroke(new BasicStroke(t == recommendedTown ? 3f : 1.5f));
            g.draw(circle);
        }
    }

    private void drawCentered(Graphics2D g, String text, int cx, int cy) {
        java.awt.FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(text);
        g.drawString(text, cx - w / 2, cy);
    }

    // ---- click handling ----

    private void handleClick(int mx, int my) {
        if (listener == null) return;

        // closest town within radius wins first (towns sit "on top of" hexes)
        int bestTown = -1;
        double bestTownDist = 14; // click tolerance in px
        for (int t = 0; t < BoardGeometry.TOTAL_TOWNS; t++) {
            java.awt.Point p = townScreenPos.get(t);
            double d = Math.hypot(p.x - mx, p.y - my);
            if (d < bestTownDist) { bestTownDist = d; bestTown = t; }
        }
        if (bestTown != -1) {
            listener.onTownClicked(bestTown);
            return;
        }

        // then closest edge within tolerance
        int[] bestEdge = null;
        double bestEdgeDist = 10;
        for (int[] edge : BoardGeometry.EDGES) {
            java.awt.Point a = townScreenPos.get(edge[0]);
            java.awt.Point b = townScreenPos.get(edge[1]);
            double d = pointToSegmentDistance(mx, my, a.x, a.y, b.x, b.y);
            if (d < bestEdgeDist) { bestEdgeDist = d; bestEdge = edge; }
        }
        if (bestEdge != null) {
            listener.onEdgeClicked(bestEdge);
            return;
        }

        // otherwise, whichever hex contains the click
        int bestHex = -1;
        double bestHexDist = scale;
        for (int h = 0; h < BoardGeometry.TOTAL_HEXES; h++) {
            java.awt.Point c = hexScreenPos.get(h);
            double d = Math.hypot(c.x - mx, c.y - my);
            if (d < bestHexDist) { bestHexDist = d; bestHex = h; }
        }
        if (bestHex != -1) {
            listener.onHexClicked(bestHex);
        }
    }

    private static double pointToSegmentDistance(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax, dy = by - ay;
        double lengthSq = dx * dx + dy * dy;
        double t = lengthSq == 0 ? 0 : ((px - ax) * dx + (py - ay) * dy) / lengthSq;
        t = Math.max(0, Math.min(1, t));
        double projX = ax + t * dx, projY = ay + t * dy;
        return Math.hypot(px - projX, py - projY);
    }
}
