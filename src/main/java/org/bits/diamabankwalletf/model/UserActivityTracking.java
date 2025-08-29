package org.bits.diamabankwalletf.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "USER_ACTIVITY_TRACKING")
@IdClass(UserActivityTracking.UserActivityTrackingId.class)
public class UserActivityTracking {

    @Id
    @Column(name = "OP_TIMESTAMP")
    private Timestamp opTimestamp;

    @Id
    @Column(name = "USER_CODE", length = 50)
    private String userCode;

    @Column(name = "USER_IP", length = 32)
    private String userIp;

    @Column(name = "OP_LINK", length = 1024)
    private String opLink;

    @Column(name = "OP_DESC", length = 256)
    private String opDesc;

    @Column(name = "DEVICE_ID", length = 256)
    private String deviceId;

    @Column(name = "USER_AGENT", length = 512)
    private String userAgent;

    // Composite key
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserActivityTrackingId implements Serializable {
        private Timestamp opTimestamp;
        private String userCode;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UserActivityTrackingId that = (UserActivityTrackingId) o;

            if (!opTimestamp.equals(that.opTimestamp)) return false;
            return userCode.equals(that.userCode);
        }

        @Override
        public int hashCode() {
            int result = opTimestamp.hashCode();
            result = 31 * result + userCode.hashCode();
            return result;
        }
    }
}
