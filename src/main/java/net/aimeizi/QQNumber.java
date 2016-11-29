package net.aimeizi;

import java.io.Serializable;

import org.kohsuke.stapler.DataBoundConstructor;

public class QQNumber implements Serializable {

    private static final long serialVersionUID = 1L;
    private String number;
    private QQType type;

    @DataBoundConstructor
    public QQNumber(String number, QQType type) {
        super();
        this.number = number;
        this.type = type;
    }

    public QQNumber() {
        super();
    }

    public String getNumber() {
        return number;
    }

    public QQType getType() {
        return type;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public void setType(QQType type) {
        this.type = type;
    }

    /**
     * 根据QQ号码类型返回QQ消息发送请求url
     *
     * @return
     */
    public String GetUrlString() {
        if (type == QQType.Qun) {
            return "send_group_message?gnumber=" + number;
        } else {
            return "send_message?qq=" + number;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("QQNumber [ ");
        builder.append("type=");
        builder.append(type);
        builder.append(" number=");
        builder.append(number + " ]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((number == null) ? 0 : number.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        QQNumber other = (QQNumber) obj;
        if (number == null) {
            if (other.number != null)
                return false;
        } else if (!number.equals(other.number))
            return false;
        return true;
    }

}
