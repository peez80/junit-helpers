package de.stiffi.testing.junit.rules.waitfor;

import de.stiffi.testing.junit.helpers.ProcessHelper;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.util.regex.Pattern;

public class DockerLogPullerRule extends ExternalResource {


    private String containerName;
    private Pattern regex;
    private long timeoutMs;
    private long pollInterval;
	private boolean showStartupLogs;

    public DockerLogPullerRule(String containerName, String regex, long timeoutMs, long pollInterval) {
        this.containerName = containerName;
        this.regex = Pattern.compile(regex);
        this.timeoutMs = timeoutMs;
        this.pollInterval = pollInterval;
    }

    public DockerLogPullerRule(String containerName, String regex, long timeoutMs, long pollInterval, boolean showStartupLogs) {
        this(containerName, regex, timeoutMs, pollInterval);
        this.showStartupLogs = showStartupLogs;
    }

    @Override
    protected void before() throws Throwable {
        System.out.println("Polling Logs of container " + containerName + " for regex " + regex.pattern());
        poll();
        System.out.println("Pattern " + regex.pattern() + " found in Container " + containerName + " logs");
    }

    private void poll() throws IOException, InterruptedException {
        long stopTime = System.currentTimeMillis() + timeoutMs;
        String logs = null;
        boolean started = false;
        while (System.currentTimeMillis() < stopTime && !started) {
            Thread.sleep(pollInterval);
            
            String cmd = "docker logs " + containerName;
            logs = ProcessHelper.execute(cmd);
            started = regex.matcher(logs).find();
        }        

    	if (showStartupLogs) {
    		System.out.println("===============Startup logs for " + containerName);
    		System.out.println(logs);
    		System.out.println("===============END startup logs for " + containerName);    		
    	}         

    	if (!started) {
    		throw new TimedoutException("Regex " + regex.pattern() + " " + " not found in logs for container " + containerName + " within " + (timeoutMs/1000) + "sec. \n Last 50 Log Lines:\n" + gatherLastDockerLogLines());
    	}
    }

    private String gatherLastDockerLogLines() throws IOException {
        return ProcessHelper.execute("docker logs --tail=50 " + containerName);
    }
}
