#include "Graph.h"

QList<Graph::Point>& Graph::getPointList() {
	return pointList;
}

QList<Graph::Edge>& Graph::getEdgeList() {
	return edgeList;
}

void Graph::addNewPoint(qint32 x, qint32 y) {
	Point point = {
		x, y,
		false, false, false,
		0
	};
	pointList.append(point);
}

void Graph::removePoint(int index) {
	pointList.removeAt(index);
	for (int i = index; i < pointList.size(); i++) {
		auto& point = pointList[i];
		if (point.after == index) {
			point.after = -1;
		} else if (point.after > index) {
			point.after--;
		}
	}

	for (int i = 0; i < edgeList.size();) {
		auto& edge = edgeList[i];
		if (edge.pointA == index || edge.pointB == index) {
			edgeList.removeAt(i);
			continue;
		}
		if (edge.pointA >= index) edge.pointA--;
		if (edge.pointB >= index) edge.pointB--;
		i++;
	}
}

void Graph::addEdge(int pointA, int pointB) {
	if (pointA > pointB) {
		addEdge(pointB, pointA);
		return;
	}
	assert(pointA != pointB);
	assert(pointB < pointList.size());
	for (auto& edge : edgeList) {
		if (edge.pointA == pointA && edge.pointB == pointB)
			return;
	}
	Edge edge = { pointA, pointB, Edge::None };
	edgeList.append(edge);
}

void Graph::removeEdge(int index) {
	edgeList.removeAt(index);
}