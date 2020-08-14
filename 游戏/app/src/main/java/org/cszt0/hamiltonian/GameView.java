package org.cszt0.hamiltonian;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

public class GameView extends View {
    private final Paint paint;
    private Graph graph;

    private final LinkedList<Integer> road;
    private int[] useRoadCount;
    private int[] pointReachCount;
    private int[] pointEnergyRequire;
    private boolean[][] edgeAccess;
    private int start;
    private int end;
    private int energy;
    private int depth;
    private boolean pressed;
    private float pressX, pressY;

    private GameListener listener;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);
        road = new LinkedList<>();
    }

    public void loadGraph(String path) {
        try {
            AssetManager assetManager = getContext().getAssets();
            InputStream inputStream = assetManager.open(path);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            graph = Graph.readFromStream(bufferedInputStream);
            generateGraphData();
            postInvalidate();
            setEnabled(true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void generateGraphData() {
        road.clear();
        int size = graph.pointList.size();
        start = -1;
        end = -1;
        energy = 0;
        depth = 0;
        // 寻找起点、终点
        for (Graph.Point point : graph.pointList) {
            if (point.isStart) start = point.index;
            if (point.isEnd) end = point.index;
            depth += point.isTwice ? 2 : 1;
        }
        // 点路过次数、能量
        pointReachCount = new int[size];
        pointEnergyRequire = new int[size];
        for (Graph.Point point : graph.pointList) {
            pointReachCount[point.index] = point.isTwice ? 2 : 1;
            pointEnergyRequire[point.index] = point.energy;
        }
        // 邻接矩阵
        useRoadCount = new int[graph.edgeList.size()];
        edgeAccess = new boolean[size][size];
        for (Graph.Edge edge : graph.edgeList) {
            if (edge.direct != Graph.Edge.Direct.A2B) {
                edgeAccess[edge.pointB][edge.pointA] = true;
            }
            if (edge.direct != Graph.Edge.Direct.B2A) {
                edgeAccess[edge.pointA][edge.pointB] = true;
            }
        }
        // 起点
        if (start != -1) {
            gotoPointImpl(start);
        }
    }

    public void clearGraph() {
        graph = null;
        postInvalidate();
        setEnabled(true);
    }

    public void gotoLast() {
        if (!isEnabled()) return;
        if (road.isEmpty()) return;
        int oldEnergy = energy;
        gotoLastImpl();
        if (road.isEmpty() && start != -1) {
            gotoPointImpl(start);
        }
        if (oldEnergy != energy) {
            onEnergyChange();
        }
        postInvalidate();
    }

    private void gotoLastImpl() {
        int index = road.removeLast();
        pointReachCount[index]++;
        depth++;
        energy -= pointEnergyRequire[index];
        if (!road.isEmpty()) {
            int from = road.getLast();
            int to = index;
            if (from > to) {
                to = from;
                from = index;
            }
            for (int i = 0; i < graph.edgeList.size(); i++) {
                Graph.Edge edge = graph.edgeList.get(i);
                if (edge.pointA == from && edge.pointB == to) {
                    useRoadCount[i]--;
                    break;
                }
            }
        }
    }

    public void clearRoad() {
        setEnabled(true);
        int oldEnergy = energy;
        while (!road.isEmpty()) gotoLastImpl();
        if (start != -1) gotoPointImpl(start);
        if (oldEnergy != energy) {
            onEnergyChange();
        }
        postInvalidate();
    }

    public void setOnFinishListener(GameListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Graph graph = this.graph;
        if (graph != null) {
            float widgetWidth = getWidth();
            float widgetHeight = getHeight();
            float radius = Math.min(widgetWidth, widgetHeight) / 20;
            float lineWidth = radius / 2;
            paint.setTextSize(radius * 1.2F);
            float textOffsetY = (paint.descent() - paint.ascent()) / 4;

            List<Graph.Point> pointList = graph.pointList;
            List<Graph.Edge> edgeList = graph.edgeList;

            // 绘制线
            for (int i = 0; i < edgeList.size(); i++) {
                if (useRoadCount[i] > 0) {
                    paint.setColor(0xff00ff99);
                } else {
                    paint.setColor(0xffdddddd);
                }
                paint.setStrokeWidth(lineWidth);
                Graph.Edge edge = edgeList.get(i);
                Graph.Point pointA = pointList.get(edge.pointA);
                Graph.Point pointB = pointList.get(edge.pointB);

                float ax = widgetWidth * pointA.x / 0x7fffffff;
                float ay = widgetHeight * pointA.y / 0x7fffffff;
                float bx = widgetWidth * pointB.x / 0x7fffffff;
                float by = widgetHeight * pointB.y / 0x7fffffff;
                canvas.drawLine(ax, ay, bx, by, paint);
                if (edge.direct != Graph.Edge.Direct.None) {
                    float cx = (ax + bx) / 2;
                    float cy = (ay + by) / 2;
                    float length = lineWidth * 2;
                    final float angle = 0.5F;
                    paint.setStrokeWidth(lineWidth * 0.5f);
                    double angle1 = Math.atan2(ay - by, ax - bx);
                    double angle2 = Math.atan2(ax - bx, ay - by);
                    if (edge.direct == Graph.Edge.Direct.A2B) {
                        double a = angle1 - angle - Math.PI;
                        double b = angle2 - angle - Math.PI;
                        float x1 = (float) (cx - length * Math.cos(a));
                        float y1 = (float) (cy - length * Math.sin(a));
                        float x2 = (float) (cx - length * Math.sin(b));
                        float y2 = (float) (cy - length * Math.cos(b));
                        canvas.drawLine(cx, cy, x1, y1, paint);
                        canvas.drawLine(cx, cy, x2, y2, paint);
                    } else {
                        double a = angle1 - angle;
                        double b = angle2 - angle;
                        float x1 = (float) (cx - length * Math.cos(a));
                        float y1 = (float) (cy - length * Math.sin(a));
                        float x2 = (float) (cx - length * Math.sin(b));
                        float y2 = (float) (cy - length * Math.cos(b));
                        canvas.drawLine(cx, cy, x1, y1, paint);
                        canvas.drawLine(cx, cy, x2, y2, paint);
                    }
                }
            }

            if (pressed && !road.isEmpty()) {
                Graph.Point lastPoint = graph.pointList.get(road.getLast());
                paint.setColor(0xff00ff99);
                float ax = widgetWidth * lastPoint.x / 0x7fffffff;
                float ay = widgetHeight * lastPoint.y / 0x7fffffff;
                float bx = pressX;
                float by = pressY;
                canvas.drawLine(ax, ay, bx, by, paint);
            }

            // 绘制点
            for (int i = 0; i < pointList.size(); i++) {
                Graph.Point point = pointList.get(i);
                float x = widgetWidth * point.x / 0x7fffffff;
                float y = widgetHeight * point.y / 0x7fffffff;

                if (!road.isEmpty() && road.getLast() == i) {
                    paint.setColor(0xff000000);
                    float r = radius * 1.1F;
                    if (point.isTwice) {
                        canvas.drawRect(x - r, y - r, x + r, y + r, paint);
                    } else {
                        canvas.drawCircle(x, y, r, paint);
                    }
                }

                int color;
                if (pointReachCount[i] == 0 && (road.size() > 1 || !point.isStart)) {
                    color = 0xff33ff00;
                } else if (point.isStart && road.size() == 1) {
                    color = 0xff00ffff;
                } else if (point.isEnd) {
                    color = 0xffff6600;
                } else {
                    color = 0xffff99ff;
                }
                paint.setColor(color);

                if (point.isTwice) {
                    canvas.drawRect(x - radius, y - radius, x + radius, y + radius, paint);
                } else {
                    canvas.drawCircle(x, y, radius, paint);
                }

                if (point.energy != 0) {
                    paint.setColor(Color.BLACK);
                    canvas.drawText(String.valueOf(point.energy), x, y + textOffsetY, paint);
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            pressed = false;
            return false;
        }
        pressX = event.getX();
        pressY = event.getY();
        int action = event.getAction();
        if (action == MotionEvent.ACTION_UP) {
            pressed = false;
        } else {
            pressed = true;
            int index = getTouchPoint();
            if (index != -1) {
                gotoPoint(index);
            }
        }
        postInvalidate();
        return true;
    }

    private void gotoPoint(int index) {
        int oldEnergy = energy;
        if (road.isEmpty()) {
            if (pointEnergyRequire[index] >= 0 && (depth == 1 || pointReachCount[index] > 1 || index != end)) {
                gotoPointImpl(index);
            }
        } else {
            int from = road.getLast();
            if (edgeAccess[from][index] && pointReachCount[index] > 0 && (depth == 1 || index != end) && energy + pointEnergyRequire[index] >= 0) {
                gotoPointImpl(index);
            }
        }
        if (oldEnergy != energy) {
            onEnergyChange();
        }
    }

    private void gotoPointImpl(int index) {
        if (!road.isEmpty()) {
            int from = road.getLast();
            int to = index;
            if (from > to) {
                to = from;
                from = index;
            }
            for (int i = 0; i < graph.edgeList.size(); i++) {
                Graph.Edge edge = graph.edgeList.get(i);
                if (edge.pointA == from && edge.pointB == to) {
                    useRoadCount[i]++;
                    break;
                }
            }
        }
        road.add(index);
        pointReachCount[index]--;
        depth--;
        energy += pointEnergyRequire[index];
        if (depth == 0) {
            onFinish();
        }
    }

    private int getTouchPoint() {
        float x = pressX, y = pressY;
        float widgetWidth = getWidth();
        float widgetHeight = getHeight();
        float radius = Math.min(widgetWidth, widgetHeight) / 20;
        float r2 = radius * radius;

        List<Graph.Point> pointList = graph.pointList;
        for (int i = 0; i < pointList.size(); i++) {
            Graph.Point point = pointList.get(i);
            float px = widgetWidth * point.x / 0x7fffffff;
            float py = widgetHeight * point.y / 0x7fffffff;

            float dx = x - px, dy = y - py;
            float distance2 = dx * dx + dy * dy;
            if (distance2 < r2) return i;
        }
        return -1;
    }

    private void onFinish() {
        setEnabled(false);
        if (listener != null) {
            listener.onFinish();
        }
    }

    private void onEnergyChange() {
        if (listener != null) {
            listener.onEnergyChange(energy);
        }
    }

    public interface GameListener {
        void onEnergyChange(int energy);

        void onFinish();
    }
}
