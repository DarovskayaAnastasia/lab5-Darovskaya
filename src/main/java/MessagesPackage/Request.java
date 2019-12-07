package MessagesPackage;

public class Request {

    private String url;
    private int index;

    public Request(String url, int index) {
        this.url = url;
        this.index = index;
    }

    public String getUrl() {
        return url;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
