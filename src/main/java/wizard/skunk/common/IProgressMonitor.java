package wizard.skunk.common;

public interface IProgressMonitor {
	void beginTask(String name, int totalWork);
	void setIndeterminate(boolean value);
	void subTask(String name);
	void worked(int work);
	void done();
	void error(String errorMessage);
}
