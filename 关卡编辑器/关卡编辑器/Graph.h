#pragma once
#include <QList>

class Graph
{
public:
	struct Point
	{
		qint32 x, y;
		bool isStart;
		bool isEnd;
		bool isTwice;
		int after;
	};

	struct Edge
	{
		int pointA, pointB;
		enum Direct
		{
			None,
			A2B,
			B2A
		} direct;
	};

	QList<Point>& getPointList();
	QList<Edge>& getEdgeList();

	void addNewPoint(qint32 x, qint32 y);
	void removePoint(int index);
	void addEdge(int pointA, int pointB);
	void removeEdge(int index);

private:
	QList<Point> pointList;
	QList<Edge> edgeList;
};

