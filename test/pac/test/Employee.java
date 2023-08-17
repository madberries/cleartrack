package pac.test;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

// To be used by hibernate for injection only.
@Entity
@Table(name = "employees")
public class Employee implements Serializable {
  private static final long serialVersionUID = 175130057244455422L;

  // "id", "VARCHAR(20)", //
  // "name", "VARCHAR(20)", //
  // "hired", "DATE", //
  // "email", "VARCHAR(30)", //
  // "position", "VARCHAR(20)", //
  // "password", "VARCHAR(20)" };
  String id;
  private Integer count;
  String name;
  Date hired;
  String email;
  String position;
  String password;

  @Id
  // @Column(name="ID")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  // @Column(name="name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Date getHired() {
    return hired;
  }

  public void setHired(Date hired) {
    this.hired = hired;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPosition() {
    return position;
  }

  public void setPosition(String position) {
    this.position = position;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  Integer getCount() {
    return count;
  }

  void setCount(Integer count) {
    this.count = count;
  }

  public String toString() {
    String ret = "id = " + getId() + ", " + "name = " + getName() + ", " + "count = " + getCount()
        + ", " + "password = " + getPassword() + ", " + "email = " + getEmail() + ", "
        + "position = " + getPosition() + ", " + "hired = " + getHired();
    return ret;
  }

}
