package model;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import util.MysqlUtil;
import vo.CompanyVO;

public class CompanyDAO {
	Connection conn; 
	Statement st;
	PreparedStatement pst; 
	CallableStatement cst; 
	ResultSet rs;
	
	public List<CompanyVO> companyList() {
		String sql = "select * from company order by company_id";
		List<CompanyVO> companyList = new ArrayList<>();
		
		conn = MysqlUtil.getConnection();
		
		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql);
			
			while(rs.next()) {
				CompanyVO company = makeCompany(rs);
				companyList.add(company);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			MysqlUtil.dbDisconnect(rs, st, conn);
		}
		
		
		return companyList;
	}

	// 은빈: company 객체 생성
	private CompanyVO makeCompany(ResultSet rs) throws SQLException {
		CompanyVO company = new CompanyVO();
		
		company.setCompany_commission(rs.getDouble("Company_commission"));
		company.setCompany_id(rs.getInt("Company_id"));
		company.setCompany_name(rs.getString("Company_name"));
		company.setCompany_pw(rs.getString("Company_pw"));
		company.setCompany_status(rs.getString("Company_status").charAt(0));
		
		return company;
	}
}
