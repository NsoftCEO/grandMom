package ko.dh.goot.common.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import ko.dh.goot.payment.domain.RefundStatus;
/*
@MappedTypes(RefundStatus.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class RefundStatusTypeHandler extends BaseTypeHandler<RefundStatus> {

    @Override
    public void setNonNullParameter(
            PreparedStatement ps,
            int i,
            RefundStatus parameter,
            JdbcType jdbcType
    ) throws SQLException {
        ps.setString(i, parameter.name());
    }

    @Override
    public RefundStatus getNullableResult(ResultSet rs, String columnName)
            throws SQLException {
        return RefundStatus.from(rs.getString(columnName));
    }

    @Override
    public RefundStatus getNullableResult(ResultSet rs, int columnIndex)
            throws SQLException {
        return RefundStatus.from(rs.getString(columnIndex));
    }

    @Override
    public RefundStatus getNullableResult(CallableStatement cs, int columnIndex)
            throws SQLException {
        return RefundStatus.from(cs.getString(columnIndex));
    }
}
*/