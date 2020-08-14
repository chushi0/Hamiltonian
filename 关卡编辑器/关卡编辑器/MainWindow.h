#pragma once

#include <QtWidgets/QMainWindow>
#include "ui_MainWindow.h"

class MainWindow : public QMainWindow
{
	Q_OBJECT

public:
	MainWindow(QWidget* parent = Q_NULLPTR);

private:
	Ui::MainWindowClass ui;

	Graph* graph;
	int index;

	bool checkConnect(Graph*);
	void dfsSearchAnswer(int size, int* pointReachCount, int** edgeAccess, int endPointRequire, int* afterRequire, int energy, int lastPoint, int depth, int* answerCount);
};
