package wizard.skunk.common;

import java.text.SimpleDateFormat;
import java.util.Date;

public class StandardMonitor implements IProgressMonitor{
	private String taskName;
	private int totalWork;
	private int currentWork;

	private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

	@Override
	public void beginTask(String name, int totalWork) {
		taskName = name;
		this.totalWork = totalWork;
		this.currentWork = 1;
	}

	@Override
	public void setIndeterminate(boolean value) {

	}

	@Override
	public void subTask(String name) {
		print(taskName + " - " + name + "(" + currentWork + "/" + totalWork + ")");
	}

	@Override
	public void worked(int work) {
		currentWork += work;
	}

	@Override
	public void done() {

	}

	@Override
	public void error(String errorMessage) {
		print(errorMessage);
	}

	private void print(String message){
		Date currentDate = new Date();
		System.out.println(formatter.format(currentDate) + " - " + message);
	}
}
