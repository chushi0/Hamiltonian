#include "MainWindow.h"
#include <QFileDialog>
#include <QMessageBox>

MainWindow::MainWindow(QWidget* parent) : QMainWindow(parent), graph(nullptr) {
	ui.setupUi(this);
	connect(ui.optionSelect, &QRadioButton::toggled, [this](bool checked) {
		if (checked)
			ui.graphWidget->changeOption(GraphWidget::Option::Select);
		});
	connect(ui.optionMovePoint, &QRadioButton::toggled, [this](bool checked) {
		if (checked)
			ui.graphWidget->changeOption(GraphWidget::Option::MovePoint);
		});
	connect(ui.optionNewPoint, &QRadioButton::toggled, [this](bool checked) {
		if (checked)
			ui.graphWidget->changeOption(GraphWidget::Option::NewPoint);
		});
	connect(ui.optionNewEdge, &QRadioButton::toggled, [this](bool checked) {
		if (checked)
			ui.graphWidget->changeOption(GraphWidget::Option::NewEdge);
		});
	connect(ui.optionDeletePoint, &QRadioButton::toggled, [this](bool checked) {
		if (checked)
			ui.graphWidget->changeOption(GraphWidget::Option::DeletePoint);
		});
	connect(ui.optionDeleteEdge, &QRadioButton::toggled, [this](bool checked) {
		if (checked)
			ui.graphWidget->changeOption(GraphWidget::Option::DeleteEdge);
		});

	connect(ui.graphWidget, &GraphWidget::onSelectPoint, [this](auto graph, auto index) {
		this->graph = nullptr;
		ui.pointEditor->setEnabled(true);
		ui.edgeEditor->setEnabled(false);
		auto point = graph->getPointList()[index];
		ui.pointStart->setChecked(point.isStart);
		ui.pointEnd->setChecked(point.isEnd);
		ui.pointTwice->setChecked(point.isTwice);
		ui.pointAfter->setChecked(point.after != 0);
		ui.pointAfterInput->setText(QString::number(point.after));
		this->graph = graph;
		this->index = index;
		});
	connect(ui.graphWidget, &GraphWidget::onSelectEdge, [this](auto graph, auto index) {
		this->graph = nullptr;
		ui.pointEditor->setEnabled(false);
		ui.edgeEditor->setEnabled(true);
		this->graph = graph;
		this->index = index;
		});
	connect(ui.graphWidget, &GraphWidget::onClearSelect, [this] {
		ui.pointEditor->setEnabled(false);
		ui.edgeEditor->setEnabled(false);
		graph = nullptr;
		});
	connect(ui.pointStart, &QCheckBox::toggled, [this](bool checked) {
		if (graph) {
			graph->getPointList()[index].isStart = checked;
			ui.graphWidget->update();
		}
		});
	connect(ui.pointEnd, &QCheckBox::toggled, [this](bool checked) {
		if (graph) {
			graph->getPointList()[index].isEnd = checked;
			ui.graphWidget->update();
		}
		});
	connect(ui.pointTwice, &QCheckBox::toggled, [this](bool checked) {
		if (graph) {
			graph->getPointList()[index].isTwice = checked;
			ui.graphWidget->update();
		}
		});
	connect(ui.pointAfter, &QCheckBox::toggled, [this](bool checked) {
		ui.pointAfterInput->setEnabled(checked);
		if (graph) {
			if (checked) {
				bool ok;
				int num = ui.pointAfterInput->text().toInt(&ok);
				if (ok) {
					graph->getPointList()[index].after = num;
				} else {
					graph->getPointList()[index].after = 0;
				}
			} else {
				graph->getPointList()[index].after = 0;
			}
			ui.graphWidget->update();
		}
		});
	connect(ui.pointAfterInput, &QLineEdit::textChanged, [this](QString string) {
		if (graph) {
			bool ok;
			int num = string.toInt(&ok);
			if (ok) {
				graph->getPointList()[index].after = num;
			} else {
				graph->getPointList()[index].after = 0;
			}
			ui.graphWidget->update();
		}
		});

	connect(ui.edgeChangeDirect, &QPushButton::pressed, [this] {
		if (graph) {
			auto& direct = graph->getEdgeList()[index].direct;
			int numDirect = direct;
			numDirect = (numDirect + 1) % 3;
			direct = (Graph::Edge::Direct) numDirect;
			ui.graphWidget->update();
		}
		});
	connect(ui.createNew, &QPushButton::pressed, [this] {
		ui.graphWidget->createNewGraph();
		});
	connect(ui.open, &QPushButton::pressed, [this] {
		auto filename = QFileDialog::getOpenFileName(this);
		if (filename.isEmpty()) return;
		QFile file(filename);
		file.open(QIODevice::ReadOnly);
		Graph graph;
		auto& pointList = graph.getPointList();
		auto& edgeList = graph.getEdgeList();
		uint8_t size[2];
		file.read((char*)&size, sizeof(size));
		for (int i = 0; i < size[0]; i++) {
			int32_t pos[2];
			int8_t after;
			uint8_t flag;
			file.read((char*)&pos, sizeof(pos));
			file.read((char*)&after, sizeof(after));
			file.read((char*)&flag, sizeof(flag));
			Graph::Point point;
			point.x = pos[0];
			point.y = pos[1];
			point.after = after;
			point.isStart = (flag >> 2) & 1;
			point.isEnd = (flag >> 1) & 1;
			point.isTwice = (flag) & 1;
			pointList.append(point);
		}
		for (int i = 0; i < size[1]; i++) {
			int8_t data[3];
			file.read((char*)&data, sizeof(data));
			Graph::Edge edge;
			edge.pointA = data[0];
			edge.pointB = data[1];
			edge.direct = (Graph::Edge::Direct) data[2];
			edgeList.append(edge);
		}
		file.close();
		ui.graphWidget->changeGraph(&graph);
		});
	connect(ui.save, &QPushButton::pressed, [this] {
		auto filename = QFileDialog::getSaveFileName(this);
		if (filename.isEmpty()) return;
		QFile file(filename);
		file.open(QIODevice::WriteOnly);
		auto graph = ui.graphWidget->getGraph();
		auto& pointList = graph->getPointList();
		auto& edgeList = graph->getEdgeList();
		uint8_t size[2] = { pointList.size(), edgeList.size() };
		file.write((const char*)&size, sizeof(size));
		for (int i = 0; i < size[0]; i++) {
			auto& point = pointList[i];
			int32_t pos[2] = { point.x, point.y };
			int8_t after = point.after;
			uint8_t flag = (((point.isStart << 1) | point.isEnd) << 1) | point.isTwice;
			file.write((const char*)&pos, sizeof(pos));
			file.write((const char*)&after, sizeof(after));
			file.write((const char*)&flag, sizeof(flag));
		}
		for (int i = 0; i < size[1]; i++) {
			auto& edge = edgeList[i];
			int8_t data[3] = { edge.pointA, edge.pointB, edge.direct };
			file.write((const char*)&data, sizeof(data));
		}
		file.close();
		});

	connect(ui.checkConnect, &QPushButton::pressed, [this] {
		auto graph = ui.graphWidget->getGraph();
		QString text;
		if (checkConnect(graph)) {
			text = QString::fromStdWString(L"是连通图（忽略方向）");
		} else {
			text = QString::fromStdWString(L"不是连通图（忽略方向）");
		}
		QMessageBox::information(this, QString::fromStdWString(L"检查连通"), text);
		});
	connect(ui.checkAnswer, &QPushButton::pressed, [this] {
		auto graph = ui.graphWidget->getGraph();
		if (!checkConnect(graph)) {
			QMessageBox::information(this, QString::fromStdWString(L"检查答案"), QString::fromStdWString(L"不是连通图"));
			return;
		}
		auto& pointList = graph->getPointList();
		auto& edgeList = graph->getEdgeList();
		int size = pointList.size();
		// 寻找起点、终点、长度
		int start = -1;
		int end = -1;
		int depth = 0;
		int energyCount = 0;
		for (int i = 0; i < size; i++) {
			auto& point = pointList[i];
			if (point.isStart && point.isEnd) {
				QMessageBox::information(this, QString::fromStdWString(L"检查答案"), QString::fromStdWString(L"不能同时为起点和终点"));
				return;
			}
			if (point.isStart && start != -1) {
				QMessageBox::information(this, QString::fromStdWString(L"检查答案"), QString::fromStdWString(L"存在多个起点"));
				return;
			}
			if (point.isEnd && end != -1) {
				QMessageBox::information(this, QString::fromStdWString(L"检查答案"), QString::fromStdWString(L"存在多个终点"));
				return;
			}
			if (point.isStart) start = i;
			if (point.isEnd) end = i;
			depth += point.isTwice ? 2 : 1;
			energyCount += (point.isTwice ? 2 : 1) * point.after;
		}
		if (energyCount < 0) {
			QMessageBox::information(this, QString::fromStdWString(L"检查答案"), QString::fromStdWString(L"能量和为负数"));
			return;
		}
		// 准备数据
		int* pointReachCount = new int[size];
		int** edgeAccess = new int* [size];
		for (int i = 0; i < size; i++) {
			edgeAccess[i] = new int[size];
			memset(edgeAccess[i], 0, sizeof(int) * size);
		}
		int* afterRequire = new int[size];
		for (int i = 0; i < size; i++) {
			auto& point = pointList[i];
			pointReachCount[i] = point.isTwice ? 2 : 1;
			afterRequire[i] = point.after;
		}
		for (auto& edge : edgeList) {
			if (edge.direct != Graph::Edge::A2B) {
				edgeAccess[edge.pointB][edge.pointA] = 1;
			}
			if (edge.direct != Graph::Edge::B2A) {
				edgeAccess[edge.pointA][edge.pointB] = 1;
			}
		}
		int answerCount = 0;
		// 首个顶点
		for (int i = 0; i < size; i++) {
			if (start == i || (start == -1 && end != i)) {
				if (afterRequire[i] >= 0) {
					pointReachCount[i]--;
					dfsSearchAnswer(size, pointReachCount, edgeAccess, end, afterRequire, afterRequire[i], i, depth - 1, &answerCount);
					pointReachCount[i]++;
				}
			}
		}
		QMessageBox::information(this, QString::fromStdWString(L"检查答案"), QString::fromStdWString(L"答案个数：%1").arg(answerCount));
		delete[] pointReachCount;
		for (int i = 0; i < size; i++) {
			delete[] edgeAccess[i];
		}
		delete[] edgeAccess;
		delete[] afterRequire;
		});
}

bool MainWindow::checkConnect(Graph* graph) {
	auto& pointList = graph->getPointList();
	auto& edgeList = graph->getEdgeList();

	int pointCount = pointList.size();
	int* pointState = new int[pointCount];
	int reachPointCount = 1;
	memset(pointState, 0, sizeof(int) * pointCount);
	if (pointCount > 0) {
		pointState[0] = 1;
	}
	while (reachPointCount < pointCount) {
		int index;
		for (int i = 0; i < pointCount; i++) {
			if (pointState[i] == 1) {
				index = i;
				goto get_index;
			}
		}
		break;
	get_index:
		for (auto& edge : edgeList) {
			if (edge.pointA == index && pointState[edge.pointB] == 0) {
				pointState[edge.pointB] = 1;
				reachPointCount++;
			}
			if (edge.pointB == index && pointState[edge.pointA] == 0) {
				pointState[edge.pointA] = 1;
				reachPointCount++;
			}
		}
		pointState[index] = 2;
	}
	delete[] pointState;
	return reachPointCount >= pointCount;
}

void MainWindow::dfsSearchAnswer(int size, int* pointReachCount, int** edgeAccess, int endPointRequire, int* afterRequire, int energy, int lastPoint, int depth, int* answerCount) {
	if (depth == 0) {
		(*answerCount)++;
		return;
	}

	for (int i = 0; i < size; i++) {
		if (pointReachCount[i] && edgeAccess[lastPoint][i] && (depth == 1 || pointReachCount[i] >= 1 || i != endPointRequire) && energy + afterRequire[i] >= 0) {
			pointReachCount[i]--;
			dfsSearchAnswer(size, pointReachCount, edgeAccess, endPointRequire, afterRequire, energy + afterRequire[i], i, depth - 1, answerCount);
			pointReachCount[i]++;
		}
	}

}