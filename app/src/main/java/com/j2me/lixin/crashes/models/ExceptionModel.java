package com.j2me.lixin.crashes.models;

import java.util.List;

public class ExceptionModel {
	public String type;
	public String message;
	public List<StackFrame> frames;
	public List<ExceptionModel> innerExceptions;
}
