package org.cszt0.hamiltonian;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Graph {

    public final List<Point> pointList;
    public final List<Edge> edgeList;

    public Graph() {
        pointList = new ArrayList<>();
        edgeList = new ArrayList<>();
    }

    public static Graph readFromStream(InputStream inputStream) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        byte[] buf = new byte[2];
        dataInputStream.readFully(buf);
        int pointCount = buf[0];
        int edgeCount = buf[1];
        Graph graph = new Graph();
        for (int i = 0; i < pointCount; i++) {
            Point point = new Point();
            point.index = i;
            point.x = readInt(dataInputStream);
            point.y = readInt(dataInputStream);
            dataInputStream.readFully(buf);
            point.energy = buf[0];
            point.isStart = (((buf[1] >> 2) & 1) == 1);
            point.isEnd = (((buf[1] >> 1) & 1) == 1);
            point.isTwice = ((buf[1] & 1) == 1);
            graph.pointList.add(point);
        }
        buf = new byte[3];
        for (int i = 0; i < edgeCount; i++) {
            Edge edge = new Edge();
            dataInputStream.readFully(buf);
            edge.pointA = buf[0];
            edge.pointB = buf[1];
            switch (buf[2]) {
                case 0:
                    edge.direct = Edge.Direct.None;
                    break;
                case 1:
                    edge.direct = Edge.Direct.A2B;
                    break;
                case 2:
                    edge.direct = Edge.Direct.B2A;
                    break;
            }
            graph.edgeList.add(edge);
        }
        return graph;
    }

    private static int readInt(DataInputStream inputStream) throws IOException {
        byte[] buf = new byte[4];
        inputStream.readFully(buf);
        int result = 0;
        result |= buf[0] & 0xff;
        result |= (buf[1] & 0xff) << 8;
        result |= (buf[2] & 0xff) << 16;
        result |= (buf[3] & 0xff) << 24;
        return result;
    }

    public static class Point {
        public int index;
        public int x, y;
        public boolean isStart;
        public boolean isEnd;
        public boolean isTwice;
        public int energy;
    }

    public static class Edge {
        public int pointA, pointB;
        public Direct direct;

        public enum Direct {
            None,
            A2B,
            B2A;
        }
    }
}
