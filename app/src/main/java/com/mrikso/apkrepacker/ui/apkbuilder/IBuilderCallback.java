package com.mrikso.apkrepacker.ui.apkbuilder;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public interface IBuilderCallback {

    void setTaskStepInfo(TaskStepInfo taskStepInfo);

    void taskFailed(String str);

    void taskSucceed(File file);

    void taskTime(long time);
}

