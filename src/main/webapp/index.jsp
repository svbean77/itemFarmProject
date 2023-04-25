
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <%@ include file="../common/commonCSS.jsp" %>
	<title>ITEM FARM</title>
</head>
<body>
	<div id="wrapper">
		<%@ include file="../common/nav.jsp" %>
		<div id="content-wrapper" class="d-flex flex-column">
			<div id="content">
				<%@ include file="../common/header.jsp" %>
				
				<div class="container-fluid">
					<h1>Welcome!</h1>
						<hr>
						<a href="/auth/loginCheck.do">로그인 먼저 갔다와</a>
						<hr>
						<h2>manager: ${managerUser}</h2>
						<h2>null?: ${managerUser == null}</h2>
						<h2>""?: ${managerUser == ""}</h2>
						<h2>company: ${companyUser}</h2>
						<h2>comId: ${comId}</h2>
						<h2>comName: ${comName}</h2>
				</div>
			</div>
			<%@ include file="../common/footer.jsp" %>
		</div> 
	</div>
	<%@ include file="../common/commonETC.jsp" %>
	<%@ include file="../common/commonJS.jsp" %>
</body>
</html>