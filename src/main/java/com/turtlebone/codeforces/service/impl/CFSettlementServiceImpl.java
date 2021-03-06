package com.turtlebone.codeforces.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSONObject;
import com.turtlebone.codeforces.bean.UserResult;
import com.turtlebone.codeforces.bean.WeeklySummary;
import com.turtlebone.codeforces.entity.CFSubmission;
import com.turtlebone.codeforces.mapper.CFUserMapper;
import com.turtlebone.codeforces.model.CFSubmissionModel;
import com.turtlebone.codeforces.repository.CFSubmissionRepository;
import com.turtlebone.codeforces.service.CFSettlementService;
import com.turtlebone.codeforces.service.CFTaskService;
import com.turtlebone.codeforces.service.WeeklyTaskService;
import com.turtlebone.core.util.BeanCopyUtils;
import com.turtlebone.core.util.DateUtil;
//import com.turtlebone.core.util.SendHTTPUtil;
import com.turtlebone.core.util.StringUtil;

@Service
public class CFSettlementServiceImpl implements CFSettlementService {
	private static Logger logger = LoggerFactory.getLogger(CFSettlementServiceImpl.class);
	
	@Autowired
	private WeeklyTaskService weeklyTaskService;
	@Autowired
	private CFSubmissionRepository cFSubmissionRepo;
	@Autowired
	private CFTaskService cfTaskService;
	
	@Override
	public String calculate(String from, String to) {
		if (StringUtil.isEmpty(from) || StringUtil.isEmpty(to)) {
			from = DateUtil.getLastMonday();
			to = DateUtil.getLastSunday();
		}
		WeeklySummary weeklySummary = weeklyTaskService.queryStatus(from, to);
		for (UserResult userResult : weeklySummary.getList()) {
			List<CFSubmissionModel> list = userResult.getSubmission();
			int totalCount = 0;
			for (CFSubmissionModel submission : list) {
				if ("OK".equals(submission.getResult()) && submission.getStatus() == 0) {
					String problemIndex = submission.getProblemindex();
					int count = 1;
					for (int i = 0; i < 10; i++) {
						String str = String.format("%c", 'A' + i);
						if (problemIndex.equalsIgnoreCase(str)) {
							count = i + 1;
							logger.debug("ProblemIndex = {}, 等价于{}题", str, count);							
							break;
						}
					}
					totalCount += count;
					submission.setStatus(1);	//标记为已经处理过
					CFSubmission sub = BeanCopyUtils.map(submission, CFSubmission.class);
					cFSubmissionRepo.updateByPrimaryKeySelective(sub);
				}
			}
			logger.debug("{} 完成题目数：{}", userResult.getUsername(), totalCount);	
			if (totalCount > 0) {
				cfTaskService.completeTask(CFUserMapper.getTurtleName(userResult.getUsername()), totalCount);	
			}			
		}
		return null;
	}
	
	private boolean doCompleteProblem(Integer contestId, String problemIndex, String username) {
		String url = "http://127.0.0.1:8080/task/CF/complete";
		String paramUrl = String.format("http://codeforces.com/problemset/problem/%d/%s", 
				contestId, problemIndex);
		JSONObject req = new JSONObject();
		req.put("username", username);
		req.put("type", "B");
		req.put("url", paramUrl);
		try {
			RestTemplate template = new RestTemplate();
			template.postForEntity(url, req, String.class);
//			SendHTTPUtil.callApiServer(url, "POST", req.toJSONString(), null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
