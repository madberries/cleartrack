package pac.wrap;

public interface TaintableArray {

    public int length();

    public int[] getTaint();

    public Object getValue();

}
