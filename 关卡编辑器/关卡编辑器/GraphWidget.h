#pragma once
#include <QWidget.h>
#include "Graph.h"

class GraphWidget : public QWidget
{
	Q_OBJECT

public:
	explicit GraphWidget(QWidget* parent = nullptr, Qt::WindowFlags f = Qt::WindowFlags());
	virtual ~GraphWidget() override;

	enum Option
	{
		Select,
		MovePoint,
		NewPoint,
		NewEdge,
		DeletePoint,
		DeleteEdge
	};
	void changeOption(Option option);
	void createNewGraph();
	void changeGraph(Graph*);
	Graph* getGraph();

signals:
	void onSelectPoint(Graph* graph, int index);
	void onSelectEdge(Graph* graph, int index);
	void onClearSelect();

protected:
	virtual void paintEvent(QPaintEvent*) override;
	virtual void mousePressEvent(QMouseEvent*) override;
	virtual void mouseReleaseEvent(QMouseEvent*) override;
	virtual void mouseMoveEvent(QMouseEvent*) override;

private:
	Graph* graph;

	Option option;
	bool cacheValid;
	int cacheIndex;
	struct { qint32 x, y; } cachePoint;

	int getTouchPoint(double x, double y);
	int getTouchLine(double x, double y);
	double pointToSegDist(double x, double y, double x1, double y1, double x2, double y2);
};

