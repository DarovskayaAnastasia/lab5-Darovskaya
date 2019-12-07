public class Result {

    private String url;
    private Long averageResTime;

    public Result(String url, Long averageResTime) {
        this.url = url;
        this.averageResTime = averageResTime;
    }

    public String getUrl() {
        return url;
    }

    public Long getAverageResTime() {
        return averageResTime;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setAverageResTime(Long averageResTime) {
        this.averageResTime = averageResTime;
    }
}
