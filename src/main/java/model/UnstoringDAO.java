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
import vo.UnstoringDetailVO;
import vo.UnstoringVO;

public class UnstoringDAO {

	Connection conn;
	Statement st;
	PreparedStatement pst; // ?지원
	PreparedStatement pst2; // ?지원
//	CallableStatement cst; //SP지원 (Stored Procedure 할때 필요) 
	ResultSet rs;
	int resultCount; // insert, update, delete건수
	
	
	// 주문건 상세조회
	public List<UnstoringDetailVO> selectUnstoringDetail(UnstoringVO vo, CompanyVO vo2) {
		String sql = """
				select ud.unstoring_code '주문번호', ud.product_code '상품번호', p.product_name '상품명', ud.unstoring_quantity '상품수량', u.customer_name, u.customer_address, u.tracking_number, u.unstoring_state
				from unstoring_detail ud join unstoring u on ud.unstoring_code = u.unstoring_code 
	                     join product p on p.product_code = ud.product_code
                         join company c on c.company_id = p.company_id
				where ud.unstoring_code = ? and c.company_id = ?
				""";
		List<UnstoringDetailVO> unstoringDetailList = new ArrayList<>();
		conn = MysqlUtil.getConnection();
		try {
			pst = conn.prepareStatement(sql);
			pst.setString(1, vo.getUnstoring_code());
			pst.setInt(2, vo2.getCompany_id());
			
			rs = pst.executeQuery();
			while (rs.next()) {
				UnstoringDetailVO detailVO = makeUnstoringDetail(rs);
				unstoringDetailList.add(detailVO);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			MysqlUtil.dbDisconnect(rs, pst, conn);
		}
		return unstoringDetailList;
	}
	
	
	private UnstoringDetailVO makeUnstoringDetail(ResultSet rs2) throws SQLException {
		UnstoringDetailVO detailVO = new UnstoringDetailVO();
		detailVO.setUnstoring_code(rs.getString("주문번호"));
		detailVO.setProduct_code(rs.getInt("상품번호"));
		detailVO.setProduct_name(rs.getString("상품명"));
		detailVO.setUnstoring_quantity(rs.getInt("상품수량"));
		
		// 용희 : 주문건 상세조회 페이지에서 '주문자 정보'를 주기 위해 필요했음.
		detailVO.setCustomer_name(rs.getString("u.customer_name"));
		detailVO.setCustomer_address(rs.getString("u.customer_address"));
		detailVO.setTracking_number(rs.getString("u.tracking_number"));
		detailVO.setUnstoring_state(rs.getString("u.unstoring_state"));
		
		return detailVO;
	}


	// 송장입력 버튼 
	// => (1) 입력한 송장번호로 update
	// => (2) 재고(product_stock) (-)되게끔 : 이건 트리거?? 아니면 update문을 2번?? (addBatch? or statement 2개?) 
	public int trackingNumberInput(List<UnstoringVO> list, String trkNum, List<UnstoringDetailVO> detailList) {
		String sql_track = """
				update unstoring
				set tracking_number = ?, unstoring_state = '출고완료'
				where unstoring_code = ?
				""";
		String sql_stock = """
				update product
				set product_stock = product_stock - (select unstoring_quantity
													   from unstoring_detail
													   where product_code = ? and unstoring_code = ?)
				where product_code = ?
				""";
		conn = MysqlUtil.getConnection();
		UnstoringVO unstoring = new UnstoringVO();
		UnstoringDetailVO detailVO = new UnstoringDetailVO();
		try {
			conn.setAutoCommit(false);
			pst = conn.prepareStatement(sql_track);
			pst2 = conn.prepareStatement(sql_stock);
			
			// 여러 건을 모두 update 해야 하므로 for + addBatch/executeBatch
			for(int i=0; i<list.size(); i++) {
				// sql_track
				pst.setString(1, trkNum);
				
				unstoring = list.get(i);
				System.out.println("unstoring : "+unstoring);
				pst.setString(2, unstoring.getUnstoring_code());
				
				// sql_stock
				detailVO = detailList.get(i);
				System.out.println("detailVO : "+detailVO);
				pst2.setInt(1, detailVO.getProduct_code());
				System.out.println("detailVO.getProduct_code() : "+detailVO.getProduct_code());
				pst2.setString(2, detailVO.getUnstoring_code());
				System.out.println("detailVO.getUnstoring_code() : "+detailVO.getUnstoring_code());
				pst2.setInt(3, detailVO.getProduct_code());
				
				pst.addBatch();
				pst2.addBatch();
			}
			int[] a = pst.executeBatch();
		    int[] b = pst2.executeBatch();
			conn.commit();
			
			System.out.println("DAO - 송장입력에서 a " + a.toString());
			System.out.println("DAO - 송장입력에서 b " + b.toString());
//			resultCount = pst.executeUpdate(); // 여러 건이어도 executeUpdate의 리턴값은 1인가 보네
		} catch (SQLException e) {
			resultCount = -1;
			e.printStackTrace();
		} finally {
			MysqlUtil.dbDisconnect(rs, pst, conn);
		}
		return resultCount;
	}
	
	
	// 송장번호에 해당하는 출고상세(상품코드/주문수량) 정보를 불러오기 위한
	public List<UnstoringDetailVO> selectDetailByTrkNum(List<UnstoringVO> list) {
		String sql = """
				select * from unstoring_detail where unstoring_code = ?
				""";
		conn = MysqlUtil.getConnection();
		List<UnstoringDetailVO> detailList = new ArrayList<>();
		UnstoringVO unstoring = null;
		try {
			conn.setAutoCommit(false);
			pst = conn.prepareStatement(sql);
			
			for(int i=0; i<list.size(); i++) {
				unstoring = list.get(i);
				pst.setString(1, unstoring.getUnstoring_code());
				
				rs = pst.executeQuery();
				while(rs.next()) {
					UnstoringDetailVO detailVO = new UnstoringDetailVO();
					detailVO.setUnstoring_code(rs.getString("unstoring_code"));
					detailVO.setProduct_code(rs.getInt("product_code"));
					System.out.println("rs.getInt(\"product_code\") : " + rs.getInt("product_code"));
					detailVO.setUnstoring_quantity(rs.getInt("unstoring_quantity"));
					System.out.println("rs.getInt(\"unstoring_quantity\")" + rs.getInt("unstoring_quantity"));
					detailList.add(detailVO);
				}
			}
		} catch (SQLException e) {
			System.out.println("DAO - 송장번호에 해당하는 출고상세(상품코드/주문수량) 정보를 불러오기에서 에러");
			e.printStackTrace();
		} finally {
			MysqlUtil.dbDisconnect(rs, pst, conn);
		}
		return detailList;
	}
	
	
	
	// 주문취소 버튼 => 주문상태(unstoring_state)를 '주문취소'로 update (O)
	public int cancelOrder(List<UnstoringVO> list) {
		String sql = """
				update unstoring
				set unstoring_state = '주문취소', tracking_number = 'Canceled'
				where unstoring_code = ?	
				""";
		conn = MysqlUtil.getConnection();
		UnstoringVO unstoring = new UnstoringVO();
		try {
			conn.setAutoCommit(false);
			pst = conn.prepareStatement(sql);
			
			for(int i=0; i<list.size(); i++) {
				unstoring = list.get(i);
				pst.setString(1, unstoring.getUnstoring_code());
				
				pst.addBatch();
			}
			pst.executeBatch();
			conn.commit();
			
			resultCount = pst.executeUpdate();
		} catch (SQLException e) {
			resultCount = -1;
			e.printStackTrace();
		} finally {
			MysqlUtil.dbDisconnect(rs, pst, conn);
		}
		return resultCount;
	}
	
	
	// 주문건 등록 양식에 '상품코드' 가져오기 위한
	public List<UnstoringDetailVO> selectProductCode(CompanyVO companyVO){
		String sql = """
				select distinct(product_code) '상품번호', product_name '상품명'
				from unstoring_detail join product using(product_code)
				where company_id = ?
				""";
		List<UnstoringDetailVO> detailList = new ArrayList<>();
		conn = MysqlUtil.getConnection();
		try {
			pst = conn.prepareStatement(sql);
			pst.setInt(1, companyVO.getCompany_id());

			rs = pst.executeQuery();
			while (rs.next()) {
				UnstoringDetailVO detailVO = new UnstoringDetailVO();
				detailVO.setProduct_code(rs.getInt("상품번호"));
				detailVO.setProduct_name(rs.getString("상품명"));
				detailList.add(detailVO);
			}
		} catch (SQLException e) {
			System.out.println("DAO - 상품코드 가져오는 부분에서 에러");
			e.printStackTrace();
		} finally {
			MysqlUtil.dbDisconnect(rs, pst, conn);
		}
		return detailList;
	}
	

	// 신규 주문건 등록 - insert 2개 문장을 따로 따로하여 각 테이블에 넣기
	// (참조: 과거 EmpDAO)
	public int unstoringInsert(UnstoringVO unstoring, UnstoringDetailVO detail) {
		// 단, 송장번호, 출고상태에 대해선 insert X (송장번호는 null이 될 거고, 출고상태는 내가 준 디폴트값('출고대기')으로 될
		// 듯??)
		String sql_insert_1 = """
				insert into unstoring(unstoring_code, customer_name, customer_address, order_register, unstoring_date, unstoring_memo, manager_id)
				values(?,?,?,?,?,?,?)
				""";
		String sql_insert_2 = """
				insert into unstoring_detail(unstoring_code, product_code, unstoring_quantity)
				values(?,?,?)
				""";
		conn = MysqlUtil.getConnection();
		try {
			conn.setAutoCommit(false);
			pst = conn.prepareStatement(sql_insert_1);
			pst.setString(1, unstoring.getUnstoring_code());
			pst.setString(2, unstoring.getCustomer_name());
			pst.setString(3, unstoring.getCustomer_address());
			pst.setDate(4, unstoring.getOrder_register());
			pst.setDate(5, unstoring.getUnstoring_date());
			pst.setString(6, unstoring.getUnstoring_memo());
			pst.setString(7, unstoring.getManager_id());
			int a = pst.executeUpdate();
			
			pst2 = conn.prepareStatement(sql_insert_2);
			pst2.setString(1, unstoring.getUnstoring_code());
			pst2.setInt(2, detail.getProduct_code());
			pst2.setInt(3, detail.getUnstoring_quantity());
			int b = pst2.executeUpdate();
			
			conn.commit();

			resultCount = a+b;
		} catch (SQLException e) {
			resultCount = -1;
			e.printStackTrace();
		} finally {
			MysqlUtil.dbDisconnect(rs, pst, conn);
		}
		return resultCount;
	}
	
	
	// 주문건 조회 (company_id가 100번인 기업의 주문건은 VO 형태로 여러개니까 List<>)
	public List<UnstoringVO> selectAll(CompanyVO company) { // ★★ 로그인해서 세션에 저장된 그 회사의 정보가 들어와야 하고 => 그놈의 company_id로 아래
															// sql 조회할 거니가 필요함

		String sql = """
				select distinct(u.unstoring_code), customer_name, customer_address, order_register, unstoring_date, tracking_number, unstoring_state, u.manager_id, unstoring_memo
				from unstoring u join unstoring_detail ud on u.unstoring_code = ud.unstoring_code
				 				 join product p on ud.product_code = p.product_code
				             	 join company c on c.company_id = p.company_id
				where p.company_id = ?
				""";
		List<UnstoringVO> unstoreList = new ArrayList<>();
		conn = MysqlUtil.getConnection();
		try {
			pst = conn.prepareStatement(sql);
			pst.setInt(1, company.getCompany_id());

			rs = pst.executeQuery();
			while (rs.next()) {
				UnstoringVO unstoring = makeUnstore(rs);
				unstoreList.add(unstoring);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			MysqlUtil.dbDisconnect(rs, pst, conn);
		}
		return unstoreList;
	}

	private UnstoringVO makeUnstore(ResultSet rs) throws SQLException {
		UnstoringVO unstoring = new UnstoringVO();
		unstoring.setUnstoring_code(rs.getString("Unstoring_code"));
		unstoring.setCustomer_name(rs.getString("Customer_name"));
		unstoring.setCustomer_address(rs.getString("Customer_address"));
		unstoring.setOrder_register(rs.getDate("Order_register"));
		unstoring.setUnstoring_date(rs.getDate("Unstoring_date"));
		unstoring.setTracking_number(rs.getString("Tracking_number"));
		unstoring.setUnstoring_state(rs.getNString("Unstoring_state"));
		unstoring.setManager_id(rs.getString("Manager_id"));
		unstoring.setUnstoring_memo(rs.getString("Unstoring_memo"));

		return unstoring;
	}
	
}


