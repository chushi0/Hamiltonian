#include "GraphWidget.h"
#include <QPainter>
#include "Graph.h"

#ifndef M_PI
#define M_PI (3.14159265358979323846)
#endif

GraphWidget::GraphWidget(QWidget* parent, Qt::WindowFlags f) :
	QWidget(parent, f),
	graph(new Graph),
	option(Select),
	cacheValid(false),
	cacheIndex(-1) {
}

GraphWidget::~GraphWidget() {
	delete graph;
}

void GraphWidget::changeOption(Option option) {
	if (this->option == Select && cacheValid) {
		emit onClearSelect();
		cacheValid = false;
	}
	this->option = option;
	update();
}

void GraphWidget::createNewGraph() {
	if (this->option == Select && cacheValid) {
		emit onClearSelect();
		cacheValid = false;
	}
	delete graph;
	graph = new Graph;
	update();
}

void GraphWidget::changeGraph(Graph* graph) {
	if (this->option == Select && cacheValid) {
		emit onClearSelect();
		cacheValid = false;
	}
	if (!this->graph) {
		this->graph = new Graph;
	}
	*this->graph = *graph;
	update();
}

Graph* GraphWidget::getGraph() {
	return graph;
}

void GraphWidget::paintEvent(QPaintEvent* event) {
	QWidget::paintEvent(event);
	if (graph) {
		QPainter painter(this);
		double widgetWidth = width();
		double widgetHeight = height();
		double radius = std::min(widgetWidth, widgetHeight) / 20;
		double lineWidth = radius / 2;
		painter.fillRect(1, 1, widgetWidth - 2, widgetHeight - 2, QColor(255, 255, 255));
		QFont textFont(font());
		textFont.setPointSizeF(radius * 0.9);
		painter.setFont(textFont);

		auto& pointList = graph->getPointList();
		auto& edgeList = graph->getEdgeList();

		// 绘制线
		for (int i = 0; i < edgeList.size(); i++) {
			painter.setPen(QPen(QColor(0xdd, 0xdd, 0xdd), lineWidth));
			auto& edge = edgeList[i];
			auto& pointA = pointList[edge.pointA];
			auto& pointB = pointList[edge.pointB];

			double ax = widgetWidth * pointA.x / 0x7fffffff;
			double ay = widgetHeight * pointA.y / 0x7fffffff;
			double bx = widgetWidth * pointB.x / 0x7fffffff;
			double by = widgetHeight * pointB.y / 0x7fffffff;
			if (option == Select && cacheValid && i == ~cacheIndex) {
				painter.setPen(QPen(QColor(0, 0, 0), lineWidth * 1.1));
				painter.drawLine(ax, ay, bx, by);
				painter.setPen(QPen(QColor(0xdd, 0xdd, 0xdd), lineWidth));
			}
			painter.drawLine(ax, ay, bx, by);
			if (edge.direct != Graph::Edge::None) {
				double cx = (ax + bx) / 2;
				double cy = (ay + by) / 2;
				double length = lineWidth * 2;
				constexpr double angle = 0.5;
				painter.setPen(QPen(QColor(0xdd, 0xdd, 0xdd), lineWidth * 0.5));
				if (edge.direct == Graph::Edge::A2B) {
					float x1 = cx - length * cos(atan2(ay - by, ax - bx) - angle - M_PI);
					float y1 = cy - length * sin(atan2(ay - by, ax - bx) - angle - M_PI);
					float x2 = cx - length * sin(atan2(ax - bx, ay - by) - angle - M_PI);
					float y2 = cy - length * cos(atan2(ax - bx, ay - by) - angle - M_PI);
					painter.drawLine(cx, cy, x1, y1);
					painter.drawLine(cx, cy, x2, y2);
				} else {
					float x1 = cx - length * cos(atan2(ay - by, ax - bx) - angle);
					float y1 = cy - length * sin(atan2(ay - by, ax - bx) - angle);
					float x2 = cx - length * sin(atan2(ax - bx, ay - by) - angle);
					float y2 = cy - length * cos(atan2(ax - bx, ay - by) - angle);
					painter.drawLine(cx, cy, x1, y1);
					painter.drawLine(cx, cy, x2, y2);
				}
			}
		}
		if (option == NewEdge && cacheValid) {
			painter.setPen(QPen(QColor(0xdd, 0xdd, 0xdd), lineWidth));
			auto& pointA = pointList[cacheIndex];

			double ax = widgetWidth * pointA.x / 0x7fffffff;
			double ay = widgetHeight * pointA.y / 0x7fffffff;
			double bx = widgetWidth * cachePoint.x / 0x7fffffff;
			double by = widgetHeight * cachePoint.y / 0x7fffffff;
			painter.drawLine(ax, ay, bx, by);
		}

		// 绘制点
		for (int i = 0; i < pointList.size(); i++) {
			auto& point = pointList[i];
			double x = widgetWidth * point.x / 0x7fffffff;
			double y = widgetHeight * point.y / 0x7fffffff;

			QColor color;
			if (point.isStart) {
				color = QColor(0, 0xff, 0xff);
			} else if (point.isEnd) {
				color = QColor(0xff, 0x66, 0);
			} else {
				color = QColor(0xff, 0x99, 0xff);
			}

			if (point.isTwice) {
				if (option == Select && cacheValid && i == cacheIndex) {
					painter.fillRect(x - radius * 1.1, y - radius * 1.1, 2 * radius * 1.1, 2 * radius * 1.1, QColor(0, 0, 0));
				}
				painter.fillRect(x - radius, y - radius, 2 * radius, 2 * radius, color);
			} else {
				if (option == Select && cacheValid && i == cacheIndex) {
					QPainterPath path;
					path.addEllipse(x - radius * 1.1, y - radius * 1.1, 2 * radius * 1.1, 2 * radius * 1.1);
					painter.fillPath(path, QColor(0, 0, 0));
				}
				QPainterPath path;
				path.addEllipse(x - radius, y - radius, 2 * radius, 2 * radius);
				painter.fillPath(path, color);
			}

			if (point.after) {
				painter.setPen(QColor(0, 0, 0));
				painter.drawText(x - radius * 0.5, y + radius * 0.3, QString("%1").arg(point.after));
			}
		}
		if (option == NewPoint && cacheValid) {
			double x = widgetWidth * cachePoint.x / 0x7fffffff;
			double y = widgetHeight * cachePoint.y / 0x7fffffff;

			QPainterPath path;
			path.addEllipse(x - radius, y - radius, 2 * radius, 2 * radius);
			painter.fillPath(path, QColor(0xff, 0x99, 0xff));
		}
	}
}

void GraphWidget::mousePressEvent(QMouseEvent* event) {
	auto pos = event->pos();
	switch (option) {
	case Select:
	{
		cacheIndex = getTouchPoint(pos.x(), pos.y());
		if (cacheIndex != -1) {
			emit onSelectPoint(graph, cacheIndex);
			cacheValid = true;
			break;
		}
		cacheIndex = getTouchLine(pos.x(), pos.y());
		if (cacheIndex != -1) {
			emit onSelectEdge(graph, cacheIndex);
			cacheIndex = ~cacheIndex;
			cacheValid = true;
			break;
		}
		if (cacheValid) {
			emit onClearSelect();
			cacheValid = false;
		}
		break;
	}
	case MovePoint:
	{
		cacheIndex = getTouchPoint(pos.x(), pos.y());
		if (cacheIndex == -1) break;
		cacheValid = true;
		break;
	}
	case NewPoint:
	{
		cachePoint.x = (double)pos.x() / (double)width() * (double)(qint32)0x7fffffff;
		cachePoint.y = (double)pos.y() / (double)height() * (double)(qint32)0x7fffffff;
		cacheValid = true;
		break;
	}
	case NewEdge:
	{
		cacheIndex = getTouchPoint(pos.x(), pos.y());
		if (cacheIndex == -1) break;
		cachePoint.x = (double)pos.x() / (double)width() * (double)(qint32)0x7fffffff;
		cachePoint.y = (double)pos.y() / (double)height() * (double)(qint32)0x7fffffff;
		cacheValid = true;
		break;
	}
	case DeletePoint:
	{
		cacheIndex = getTouchPoint(pos.x(), pos.y());
		if (cacheIndex != -1) {
			graph->removePoint(cacheIndex);
		}
		break;
	}
	case DeleteEdge:
	{
		cacheIndex = getTouchLine(pos.x(), pos.y());
		if (cacheIndex != -1) {
			graph->removeEdge(cacheIndex);
		}
		break;
	}
	}
	update();
}

void GraphWidget::mouseReleaseEvent(QMouseEvent* event) {
	auto pos = event->pos();
	switch (option) {
	case MovePoint:
	{
		cacheValid = false;
		break;
	}
	case NewPoint:
	{
		if (cacheValid) {
			cacheValid = false;
			graph->addNewPoint(cachePoint.x, cachePoint.y);
		}
		break;
	}
	case NewEdge:
	{
		if (cacheValid) {
			cacheValid = false;
			int endPointIndex = getTouchPoint(pos.x(), pos.y());
			if (endPointIndex == -1) break;
			if (cacheIndex == endPointIndex) break;
			graph->addEdge(cacheIndex, endPointIndex);
		}
	}
	}
	update();
}

void GraphWidget::mouseMoveEvent(QMouseEvent* event) {
	auto pos = event->pos();
	switch (option) {
	case MovePoint:
	{
		if (cacheValid) {
			auto& point = graph->getPointList()[cacheIndex];
			point.x = (double)pos.x() / (double)width() * (double)(qint32)0x7fffffff;
			point.y = (double)pos.y() / (double)height() * (double)(qint32)0x7fffffff;
		}
		break;
	}
	case NewPoint:
	case NewEdge:
	{
		if (cacheValid) {
			cachePoint.x = (double)pos.x() / (double)width() * (double)(qint32)0x7fffffff;
			cachePoint.y = (double)pos.y() / (double)height() * (double)(qint32)0x7fffffff;
		}
		break;
	}
	}
	update();
}

int GraphWidget::getTouchPoint(double x, double y) {
	double widgetWidth = width();
	double widgetHeight = height();
	double radius = std::min(widgetWidth, widgetHeight) / 20;
	double r2 = radius * radius;

	auto& pointList = graph->getPointList();
	for (int i = 0; i < pointList.size(); i++) {
		auto& point = pointList[i];
		double px = widgetWidth * point.x / 0x7fffffff;
		double py = widgetHeight * point.y / 0x7fffffff;

		double dx = x - px, dy = y - py;
		double distance2 = dx * dx + dy * dy;
		if (distance2 < r2) return i;
	}
	return -1;
}

int GraphWidget::getTouchLine(double x, double y) {
	double widgetWidth = width();
	double widgetHeight = height();
	double radius = std::min(widgetWidth, widgetHeight) / 20;

	auto& pointList = graph->getPointList();
	auto& edgeList = graph->getEdgeList();
	for (int i = 0; i < edgeList.size(); i++) {
		auto& edge = edgeList[i];
		auto& pointA = pointList[edge.pointA];
		auto& pointB = pointList[edge.pointB];
		double ax = widgetWidth * pointA.x / 0x7fffffff;
		double ay = widgetHeight * pointA.y / 0x7fffffff;
		double bx = widgetWidth * pointB.x / 0x7fffffff;
		double by = widgetHeight * pointB.y / 0x7fffffff;

		if (pointToSegDist(x, y, ax, ay, bx, by) < radius) {
			return i;
		}
	}
	return -1;
}

double GraphWidget::pointToSegDist(double x, double y, double x1, double y1, double x2, double y2) {
	double cross = (x2 - x1) * (x - x1) + (y2 - y1) * (y - y1);
	if (cross <= 0) return sqrt((x - x1) * (x - x1) + (y - y1) * (y - y1));

	double d2 = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
	if (cross >= d2) return sqrt((x - x2) * (x - x2) + (y - y2) * (y - y2));

	double r = cross / d2;
	double px = x1 + (x2 - x1) * r;
	double py = y1 + (y2 - y1) * r;
	return sqrt((x - px) * (x - px) + (py - y) * (py - y));
}