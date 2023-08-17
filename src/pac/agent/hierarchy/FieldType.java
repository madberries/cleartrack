package pac.agent.hierarchy;

import java.io.Serializable;

/**
 * This class represents a field in our hierarchy. This class is not thread-safe, but the only place
 * it needs synchronization is in ClassHierarchy.process().
 * 
 * @author jeikenberry
 */
public class FieldType implements Serializable, Comparable<FieldType> {
  private static final long serialVersionUID = -3051231337815613461L;

  protected ClassType owner;
  protected String name;
  // private String desc;
  protected boolean isSkipped, isConstant, isStatic;

  FieldType(ClassType owner, String name, String desc, boolean isConstant, boolean isStatic) {
    this.owner = owner;
    this.name = name;
    // this.desc = desc;
    this.isConstant = isConstant;
    this.isStatic = isStatic;
  }

  public String toString() {
    return owner + "." + name;
  }

  /**
   * Two field types are equal if and only if the owner and name are equal.
   */
  @Override
  public int compareTo(FieldType ft) {
    return toString().compareTo(ft.toString());
  }

  /**
   * Convert the field into XML form (useful for debugging).
   * 
   * @return String
   */
  public String toXML() {
    StringBuilder xml = new StringBuilder();
    xml.append("      <field");
    ClassType.addXMLAttribute(xml, "name", name);
    ClassType.addXMLAttribute(xml, "owner", owner.name);
    if (!isSkipped)
      ClassType.addXMLAttribute(xml, "instrumented", "true");
    if (isConstant)
      ClassType.addXMLAttribute(xml, "constant", "true");
    xml.append("/>\n");
    return xml.toString();
  }
}
