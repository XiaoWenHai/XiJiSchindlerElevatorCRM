package com.gzunicorn.struts.action.basedata;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;
import org.apache.struts.actions.DispatchAction;
import org.apache.struts.util.MessageResources;

import com.gzunicorn.common.logic.BaseDataImpl;
import com.gzunicorn.common.util.CommonUtil;
import com.gzunicorn.common.util.DataStoreException;
import com.gzunicorn.common.util.DebugUtil;
import com.gzunicorn.common.util.HibernateUtil;
import com.gzunicorn.common.util.SysConfig;
import com.gzunicorn.common.util.SysRightsUtil;
import com.gzunicorn.hibernate.basedata.city.City;
import com.gzunicorn.hibernate.basedata.customer.Customer;
import com.gzunicorn.hibernate.basedata.hotlinefaulttype.HotlineFaultType;
import com.gzunicorn.hibernate.basedata.officecomplaintcategory.OfficeComplaintCategory;
import com.gzunicorn.hibernate.basedata.officecontracttypes.OfficeContractTypes;
import com.gzunicorn.hibernate.basedata.principal.Principal;
import com.gzunicorn.hibernate.basedata.region.Region;
import com.gzunicorn.hibernate.basedata.shouldexamineitems.ShouldExamineItems;
import com.gzunicorn.hibernate.basedata.termsecurityrisks.TermSecurityRisks;
import com.gzunicorn.hibernate.sysmanager.Loginuser;
import com.gzunicorn.hibernate.viewmanager.ViewLoginUserInfo;
import com.zubarev.htmltable.DefaultHTMLTable;
import com.zubarev.htmltable.HTMLTableCache;
import com.zubarev.htmltable.action.ServeTableForm;

public class OfficeContractTypesAction extends DispatchAction {

	Log log = LogFactory.getLog(OfficeContractTypesAction.class);
	
	BaseDataImpl bd = new BaseDataImpl();
	
	/**
	 * Method execute
	 * 
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return ActionForward
	 */
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		/** **********开始用户权限过滤*********** */
		SysRightsUtil.filterModuleRight(request, response,
				SysRightsUtil.NODE_ID_FORWARD + "officecontracttypes", null);
		/** **********结束用户权限过滤*********** */

		// Set default method is toSearchRecord
		String name = request.getParameter("method");
		if (name == null || name.equals("")) {
			name = "toSearchRecord";
			return dispatchMethod(mapping, form, request, response, name);
		} else {
			ActionForward forward = super.execute(mapping, form, request,
					response);
			return forward;
		}

	}
	
	
	/**
	 * Method toSearchRecord execute, Search record
	 * 
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return ActionForward
	 */

	@SuppressWarnings("unchecked")
	public ActionForward toSearchRecord(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) {
		
		request.setAttribute("navigator.location","全质办合同类型>> 查询列表");		
		ActionForward forward = null;
		HttpSession session = request.getSession();
		ServeTableForm tableForm = (ServeTableForm) form;
		String action = tableForm.getAction();
		
		if (tableForm.getProperty("genReport") != null
				&& !tableForm.getProperty("genReport").equals("")) {
			try {

				response = toExcelRecord(form, request, response);
				forward = mapping.findForward("exportExcel");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			HTMLTableCache cache = new HTMLTableCache(session, "officeContractTypesList");

			DefaultHTMLTable table = new DefaultHTMLTable();
			table.setMapping("fOfficeContractTypes");
			table.setLength(SysConfig.HTML_TABLE_LENGTH);
			cache.updateTable(table);
			table.setSortColumn("octId");
			table.setIsAscending(true);
			cache.updateTable(table);

			if (action.equals(ServeTableForm.NAVIGATE)
					|| action.equals(ServeTableForm.SORT)) {
				cache.loadForm(tableForm);
			} else {
				table.setFrom(0);
			}
			
			cache.saveForm(tableForm);

			String octName = tableForm.getProperty("octName");
			String octId = tableForm.getProperty("octId");
			String enabledFlag = tableForm.getProperty("enabledFlag");
			
			Session hs = null;
			
			try {

				hs = HibernateUtil.getSession();

				Criteria criteria = hs.createCriteria(OfficeContractTypes.class);
				
				if (octId != null && !octId.equals("")) {
					criteria.add(Expression.like("octId", "%" + octId.trim()
							+ "%"));
				}
				
				if (octName != null && !octName.equals("")) {
					criteria.add(Expression.like("octName", "%"
							+ octName.trim() + "%"));
				}
				if (enabledFlag != null && !enabledFlag.equals("")) {
					criteria.add(Expression.eq("enabledFlag", enabledFlag));
				}
				
				if (table.getIsAscending()) {
					criteria.addOrder(Order.asc(table.getSortColumn()));
				} else {
					criteria.addOrder(Order.desc(table.getSortColumn()));
				}
				table.setVolume(criteria.list().size());// 查询得出数据记录数;

				// 得出上一页的最后一条记录数号;
				criteria.setFirstResult(table.getFrom()); // pagefirst
				criteria.setMaxResults(table.getLength());

				cache.check(table);

				List officeContractTypesList = criteria.list();

				table.addAll(officeContractTypesList);
				session.setAttribute("officeContractTypesList", table);

			} catch (DataStoreException e) {
				e.printStackTrace();
			} catch (HibernateException e1) {

				e1.printStackTrace();
			} finally {
				try {
					hs.close();
					// HibernateSessionFactory.closeSession();
				} catch (HibernateException hex) {
					log.error(hex.getMessage());
					DebugUtil.print(hex, "HibernateUtil Hibernate Session ");
				}
			}
			forward = mapping.findForward("officeContractTypesList");
		}
		return forward;
	}
	
	/**
	 * Get the navigation description from the properties file by navigation
	 * key;
	 * 
	 * @param request
	 * @param navigation
	 */

	private void setNavigation(HttpServletRequest request, String navigation) {
		Locale locale = this.getLocale(request);
		MessageResources messages = getResources(request);
		request.setAttribute("navigator.location", messages.getMessage(locale,
				navigation));
	}
	
	/**
	 * 点击查看的方法
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 */
	public ActionForward toDisplayRecord(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response) {
		
		DynaActionForm dform = (DynaActionForm) form;
		ActionErrors errors = new ActionErrors();
		request.setAttribute("navigator.location","全质办合同类型 >> 查看");
		
		ActionForward forward = null;
		
		String id =  (String) dform.get("id");
		Session hs = null;
		OfficeContractTypes officeContractTypes = null;
		if (id != null) {
			try {
				hs = HibernateUtil.getSession();
				Query query = hs.createQuery("from OfficeContractTypes o where o.octId = :octId");
				query.setString("octId", id);
				List list = query.list();
				
				if (list != null && list.size() > 0) {
					officeContractTypes = (OfficeContractTypes) list.get(0);
					dform.set("octId", officeContractTypes.getOctId());
					dform.set("octName", officeContractTypes.getOctName());
					dform.set("enabledFlag", officeContractTypes.getEnabledFlag());
					dform.set("rem", officeContractTypes.getRem());
				} else {
					errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("display.recordnotfounterror"));
				}

				if (officeContractTypes == null) {
					errors.add(ActionErrors.GLOBAL_ERROR, new ActionError(
							"display.recordnotfounterror"));
				}
			} catch (DataStoreException e) {
				e.printStackTrace();
			} catch (HibernateException e1) {
				e1.printStackTrace();
			} finally {
				try {
					hs.close();
				} catch (HibernateException hex) {
					log.error(hex.getMessage());
					DebugUtil.print(hex, "HibernateUtil Hibernate Session ");
				}
			}

			request.setAttribute("display", "yes");
			request.setAttribute("officeContractTypesBean", officeContractTypes);
			forward = mapping.findForward("officeContractTypesDisplay");

		}

		if (!errors.isEmpty()) {
			saveErrors(request, errors);
		}
		return forward;
	}
	
	/**
	 * 
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 * @throws ServletException
	 */
	
	@SuppressWarnings("unchecked")
	public ActionForward toPrepareAddRecord(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {

		request.setAttribute("navigator.location","全质办合同类型>> 添加");

		DynaActionForm dform = (DynaActionForm) form;
		if (request.getAttribute("error") == null
				|| request.getAttribute("error").equals("")) {
			dform.initialize(mapping);
			dform.set("enabledFlag", "Y");
		}
		dform.set("enabledFlag", "Y");

		//request.setAttribute("officeContractTypesList",bd.getPullDownList("enabledflag"));

		return mapping.findForward("officeContractTypesAdd");
	}
	
	/**
	 * 
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 * @throws ServletException
	 */
	
	public ActionForward toAddRecord(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
 
		DynaActionForm dform = (DynaActionForm) form;
		ActionErrors errors = new ActionErrors();
		HttpSession session = request.getSession();
		ViewLoginUserInfo userInfo = (ViewLoginUserInfo)session.getAttribute(SysConfig.LOGIN_USER_INFO);
		Session hs = null;
		Transaction tx = null;
		
		String octId = (String) dform.get("octId");
		String octName = (String) dform.get("octName");
		String enabledFlag = (String) dform.get("enabledFlag");
		String rem = (String) dform.get("rem");
		try {
			hs = HibernateUtil.getSession();
			tx = hs.beginTransaction();

			OfficeContractTypes officeContractTypes = new OfficeContractTypes();
			officeContractTypes.setOctId(octId.trim());
			officeContractTypes.setOctName(octName.trim());
			officeContractTypes.setEnabledFlag(enabledFlag.trim());
			officeContractTypes.setRem(rem.trim());
			officeContractTypes.setOperId(userInfo.getUserID());//录入人
			officeContractTypes.setOperDate(CommonUtil.getToday());//录入时间

			hs.save(officeContractTypes);
			tx.commit();
		} catch (HibernateException e2) {
			errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("officecontracttypes.insert.duplicatekeyerror"));
			try {
				tx.rollback();
			} catch (HibernateException e3) {
				log.error(e3.getMessage());
				DebugUtil.print(e3, "Hibernate Transaction rollback error!");
			}
			log.error(e2.getMessage());
			DebugUtil.print(e2, "Hibernate region Insert error!");
		} catch (Exception e1) {
			e1.printStackTrace();
			log.error(e1.getMessage());
			DebugUtil.print(e1, "Hibernate region Insert error!");
		} finally {
			try {
				hs.close();
			} catch (HibernateException hex) {
				log.error(hex.getMessage());
				DebugUtil.print(hex, "Hibernate close error!");
			}
		}

		ActionForward forward = null;
		String isreturn = request.getParameter("isreturn");
		try {
			if (isreturn != null && isreturn.equals("Y") && errors.isEmpty()) {
				// return list page
				errors.add(ActionErrors.GLOBAL_ERROR, new ActionError(
				"insert.success"));
				forward = mapping.findForward("returnList");
			} else {
				// return addnew page
				if (errors.isEmpty()) {
					errors.add(ActionErrors.GLOBAL_ERROR, new ActionError(
							"insert.success"));
				} else {
					request.setAttribute("error", "Yes");
				}
				forward = mapping.findForward("returnAdd");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (!errors.isEmpty()) {
			saveErrors(request, errors);
		}

		return forward;
	}
	
	/**
	 * 跳转到修改级别页面
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public ActionForward toPrepareUpdateRecord(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response) {
		
		request.setAttribute("navigator.location","全质办合同类型 >> 修改");
		DynaActionForm dform = (DynaActionForm) form;
		ActionErrors errors = new ActionErrors();

		ActionForward forward = null;
		String id = null;

		if (dform.get("isreturn") != null
				&& ((String) dform.get("isreturn")).equals("N")) {
			id = (String) dform.get("octId");
		} else

 {
			id = (String) dform.get("id");
		}
		
		Session hs = null;
		OfficeContractTypes officeContractTypes = null;
		if (id != null) {
			if (request.getAttribute("error") == null
					|| request.getAttribute("error").equals("")) {
				try {
					hs = HibernateUtil.getSession();
					officeContractTypes = (OfficeContractTypes) hs.get(OfficeContractTypes.class, id);

					if (officeContractTypes == null) {
						errors.add(ActionErrors.GLOBAL_ERROR, new ActionError(
								"officecontracttypes.display.recordnotfounterror"
								));
					}
				} catch (DataStoreException e) {
					e.printStackTrace();
				} catch (HibernateException e1) {
					e1.printStackTrace();
				} finally {
					try {
						hs.close();
					} catch (HibernateException hex) {
						log.error(hex.getMessage());
						DebugUtil
								.print(hex, "HibernateUtil Hibernate Session ");
					}
				}
			}
			request.setAttribute("officeContractTypesBean", officeContractTypes);
			forward = mapping.findForward("officeContractTypesModify");
}

		if (!errors.isEmpty()) {
			saveErrors(request, errors);
		}
		return forward;
	}

	
	/**
	 * 紧急级别修改
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 * @throws ServletException
	 */
	public ActionForward toUpdateRecord(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {

		DynaActionForm dform = (DynaActionForm) form;
		ActionErrors errors = new ActionErrors();
		Session hs = null;
		Transaction tx = null;
		HttpSession session = request.getSession();
		ViewLoginUserInfo userInfo = (ViewLoginUserInfo)session.getAttribute(SysConfig.LOGIN_USER_INFO);
		
		String octId = (String) dform.get("octId");
		String octName = (String) dform.get("octName");
		String enabledFlag = (String) dform.get("enabledFlag");
		String rem = (String) dform.get("rem");
		
		try {
			hs = HibernateUtil.getSession();
			tx = hs.beginTransaction();
			OfficeContractTypes officeContractTypes = (OfficeContractTypes) hs.get(OfficeContractTypes.class, (String) dform.get("octId"));
			if (dform.get("id") != null
					&& dform.get("octId") != null
					&& !((String) dform.get("id")).equals((String) dform.get("octId"))) {
				hs.delete(officeContractTypes);
				officeContractTypes = new OfficeContractTypes();
			}
			
			
			officeContractTypes.setOctId(octId.trim());
			officeContractTypes.setOctName(octName.trim());
			officeContractTypes.setEnabledFlag(enabledFlag.trim());
			officeContractTypes.setRem(rem.trim());
			officeContractTypes.setOperId(userInfo.getUserID());//录入人
			officeContractTypes.setOperDate(CommonUtil.getToday());//录入时间

			hs.save(officeContractTypes);

			tx.commit();
		} catch (HibernateException e2) {
			errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("officecontracttypes.update.duplicatekeyerror"));
			try {
				tx.rollback();
			} catch (HibernateException e3) {
				log.error(e3.getMessage());
				DebugUtil.print(e3, "Hibernate Transaction rollback error!");
			}
			log.error(e2.getMessage());
			DebugUtil.print(e2, "Hibernate region Update error!");
		} catch (Exception e1) {
			e1.printStackTrace();
			log.error(e1.getMessage());
			DebugUtil.print(e1, "Hibernate region Update error!");
		} finally {
			try {
				hs.close();
			} catch (HibernateException hex) {
				log.error(hex.getMessage());
				DebugUtil.print(hex, "Hibernate close error!");
			}
		}

		ActionForward forward = null;
		String isreturn = request.getParameter("isreturn");
		try {
			if (isreturn != null && isreturn.equals("Y") && errors.isEmpty()) {
				// return list page
				errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("update.success"));
				forward = mapping.findForward("returnList");
			} else {
				// return modify page
				if (errors.isEmpty()) {
					errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("update.success"));
				} else {
					request.setAttribute("error", "Yes");
				}
				forward = mapping.findForward("returnModify");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (!errors.isEmpty()) {
			saveErrors(request, errors);
		}
		return forward;
	}

	/**
	 * 删除紧急级别
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 * @throws ServletException
	 */
	public ActionForward toDeleteRecord(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {

		DynaActionForm dform = (DynaActionForm) form;
		ActionErrors errors = new ActionErrors();
		Session hs = null;
		Transaction tx = null;
		try {
			hs = HibernateUtil.getSession();
			tx = hs.beginTransaction();

			OfficeContractTypes officeContractTypes = (OfficeContractTypes) hs.get(OfficeContractTypes.class, (String) dform.get("id"));
			if (officeContractTypes != null) {
				hs.delete(officeContractTypes);
				 errors.add(ActionErrors.GLOBAL_ERROR, new ActionError(
					"delete.succeed"));
			}
			tx.commit();
		} catch (HibernateException e2) {
			errors.add(ActionErrors.GLOBAL_ERROR, new ActionError(
					"delete.foreignkeyerror"));
			try {
				tx.rollback();
			} catch (HibernateException e3) {
				log.error(e3.getMessage());
				DebugUtil.print(e3, "Hibernate Transaction rollback error!");
			}
			log.error(e2.getMessage());
			DebugUtil.print(e2, "Hibernate region Update error!");
		} catch (DataStoreException e1) {
			e1.printStackTrace();
			log.error(e1.getMessage());
			DebugUtil.print(e1, "Hibernate region Update error!");

		} finally {
			try {
				hs.close();
			} catch (HibernateException hex) {
				log.error(hex.getMessage());
				DebugUtil.print(hex, "Hibernate close error!");
			}
		}

		if (!errors.isEmpty()) {
			saveErrors(request, errors);
		}

		ActionForward forward = null;
		try {
			forward = mapping.findForward("returnList");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return forward;
	}
	
	/**
	 * Method toSearchRecord execute, to Excel Record 列表查询导出Excel
	 * 
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return ActionForward
	 * @throws IOException
	 */
	public HttpServletResponse toExcelRecord(ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		String naigation = new String();
		ActionForward forward = null;
		HttpSession session = request.getSession();
		ServeTableForm tableForm = (ServeTableForm) form;
		String action = tableForm.getAction();

		String octId = tableForm.getProperty("octId");
		String octName = tableForm.getProperty("octName");
		String enabledFlag = tableForm.getProperty("enabledFlag");
		
		Session hs = null;
		XSSFWorkbook wb = new XSSFWorkbook();

		try {
			hs = HibernateUtil.getSession();

			Criteria criteria = hs.createCriteria(OfficeContractTypes.class);
			if (octName != null && !octName.equals("")) {
				criteria.add(Expression.like("octName", "%" + octName.trim() + "%"));
			}
			if (octId != null && !octId.equals("")) {
				criteria.add(Expression.like("octId", "%" + octId.trim()
						+ "%"));
			}
			if (enabledFlag != null && !enabledFlag.equals("")) {
				criteria.add(Expression.eq("enabledFlag", enabledFlag));
			}

			criteria.addOrder(Order.asc("octId"));

			List roleList = criteria.list();

			XSSFSheet sheet = wb.createSheet("全质办合同类型");
			
			Locale locale = this.getLocale(request);
			MessageResources messages = getResources(request);

			if (roleList != null && !roleList.isEmpty()) {
				int l = roleList.size();
				XSSFRow row0 = sheet.createRow( 0);
				XSSFCell cell0 = row0.createCell((short)0);
				//cell0.setEncoding(HSSFCell.ENCODING_UTF_16);
				cell0.setCellValue(messages.getMessage(locale,"officecontracttypes.octId"));

				cell0 = row0.createCell((short)1);
				//cell0.setEncoding(HSSFCell.ENCODING_UTF_16);
				cell0.setCellValue(messages.getMessage(locale,"officecontracttypes.octName"));
				
				cell0 = row0.createCell((short)2);
				//cell0.setEncoding(HSSFCell.ENCODING_UTF_16);
				cell0.setCellValue(messages.getMessage(locale,"officecontracttypes.enabledflag"));
				
				cell0 = row0.createCell((short)3);
				//cell0.setEncoding(HSSFCell.ENCODING_UTF_16);
				cell0.setCellValue(messages.getMessage(locale,"officecontracttypes.rem"));
				
				cell0 = row0.createCell((short)4);
				//cell0.setEncoding(HSSFCell.ENCODING_UTF_16);
				cell0.setCellValue(messages.getMessage(locale,"officecontracttypes.operId"));
				
				cell0 = row0.createCell((short)5);
				//cell0.setEncoding(HSSFCell.ENCODING_UTF_16);
				cell0.setCellValue(messages.getMessage(locale,"officecontracttypes.operDate"));
			
				for (int i = 0; i < l; i++) {
					OfficeContractTypes officeContractTypes = (OfficeContractTypes) roleList.get(i);
					// 创建Excel行，从0行开始
					XSSFRow row = sheet.createRow( i+1);
	
					// 创建Excel列
					XSSFCell cell = row.createCell((short)0);
					//cell.setEncoding(HSSFCell.ENCODING_UTF_16);
					cell.setCellValue(officeContractTypes.getOctId());
					
					cell = row.createCell((short)1);
					//cell.setEncoding(HSSFCell.ENCODING_UTF_16);
					cell.setCellValue(officeContractTypes.getOctName());
					
					cell = row.createCell((short)2);
					//cell.setEncoding(HSSFCell.ENCODING_UTF_16);
					cell.setCellValue(CommonUtil.tranEnabledFlag(officeContractTypes.getEnabledFlag()));
					
					cell = row.createCell((short)3);
					//cell.setEncoding(HSSFCell.ENCODING_UTF_16);
					cell.setCellValue(officeContractTypes.getRem());

					cell = row.createCell((short)4);
					//cell.setEncoding(HSSFCell.ENCODING_UTF_16);
					cell.setCellValue(bd.getName(hs, "LoginUser", "userName", "userId",officeContractTypes.getOperId()));

					cell = row.createCell((short)5);
					//cell.setEncoding(HSSFCell.ENCODING_UTF_16);
					cell.setCellValue(officeContractTypes.getOperDate());
					

				}
			}
			
		} catch (DataStoreException e) {
			e.printStackTrace();
		} catch (HibernateException e1) {
			e1.printStackTrace();
		} finally {
			try {
				hs.close();
			} catch (HibernateException hex) {
				log.error(hex.getMessage());
				DebugUtil.print(hex, "HibernateUtil Hibernate Session ");
			}
		}
		
		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		response.setHeader("Content-disposition", "offline; filename="+URLEncoder.encode("全质办合同类型", "utf-8") + ".xlsx");
		wb.write(response.getOutputStream());
		
		return response;
	}

}
