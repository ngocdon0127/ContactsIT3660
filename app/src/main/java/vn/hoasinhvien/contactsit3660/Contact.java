package vn.hoasinhvien.contactsit3660;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by NgocDon on 10/5/2015.
 */
public class Contact implements Serializable{

    public static final long serialVersionUID = 1L;

    String id;
    String name;
    ArrayList<String> number;
    ArrayList<String> email;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<String> getNumber() {
        return number;
    }

    public void setNumber(ArrayList<String> number) {
        this.number = number;
    }

    public ArrayList<String> getEmail() {
        return email;
    }

    public void setEmail(ArrayList<String> email) {
        this.email = email;
    }

    public Contact(String id, String name, ArrayList<String> number, ArrayList<String> email) {
        this.id = id;
        this.name = name;
        this.number = number;
        this.email = email;
    }

    @Override
    public String toString() {
        String s = "id: " + this.getId() +  "\nname: " + this.getName();
        if (this.getNumber() != null) {
            s += "\nPhone: " + this.getNumber().size();
            for (int i = 0; i < this.getNumber().size(); i++) {
                s += "\n" + this.getNumber().get(i);
            }
        }
        if (this.getEmail() != null) {
            s += "\nEmail: " + this.getEmail().size();
            for (int i = 0; i < this.getEmail().size(); i++) {
                s += "\n" + this.getEmail().get(i);
            }
        }
        s += "\n";
        return s;
    }

    public boolean equals(Object oc){
        if (oc instanceof Contact) {
            Contact c = (Contact) oc;
//            System.out.println("call overrided equals");
            ArrayList<String> thisNumber = this.getNumber();
            ArrayList<String> thisEmail = this.getEmail();
            ArrayList<String> cNumber = c.getNumber();
            ArrayList<String> cEmail = c.getEmail();
            if ((thisEmail.size() != cEmail.size()) || (thisNumber.size() != cNumber.size()))
                return false;
            for (int i = 0; i < thisEmail.size(); i++) {
                if (!cEmail.contains(thisEmail.get(i)))
                    return false;
            }
            for (int i = 0; i < thisNumber.size(); i++) {
                if (!cNumber.contains(thisNumber.get(i)))
                    return false;
            }
            for (int i = 0; i < cEmail.size(); i++) {
                if (!thisEmail.contains(cEmail.get(i)))
                    return false;
            }
            for (int i = 0; i < cNumber.size(); i++) {
                if (!thisNumber.contains(cNumber.get(i)))
                    return false;
            }
            return true;
        }
        return false;
    }

    public boolean isNull(){
        if ((this.getName() == null) && (this.getNumber().size() < 1) && (this.getEmail().size() < 1))
            return true;
        return false;
    }

//    @Override
//    public int hashCode() {
//        return id.hashCode();
//    }
}
