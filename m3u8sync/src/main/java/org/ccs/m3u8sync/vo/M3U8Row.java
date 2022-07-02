package org.ccs.m3u8sync.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class M3U8Row {
	private String content;
	/**
	 * 是不是#EXTINF标签
	 */
	private boolean isExtinf;
	/**
	 * ts时长，毫秒
	 */
	private long tsTimes;
	/**
	 * 开始时间节点，毫秒
	 */
	private long startTimeNode;
	/**
	 * 结束时间节点，毫秒
	 */
	private long endTimeNode;
	/**
	 * 行号索引，从0开始
	 */
	private int rowIndex;
}
