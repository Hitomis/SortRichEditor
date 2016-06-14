package com.hitomi.sortricheditor.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.List;

public abstract class BaseAdapterHelper<T> extends BaseAdapter {
	
	private Context context;
	
	private List<T> dataList;
	
	private final int itemLayoutID;
	
	public BaseAdapterHelper(Context context, List<T> dataList, int itemLayoutID) {
		this.context = context;
		this.dataList = dataList;
		this.itemLayoutID = itemLayoutID;
	}

	@Override
	public int getCount() {
		return dataList.size();
	}

	@Override
	public T getItem(int position) {
		return dataList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final ViewHolder viewHolder = getViewHolder(convertView, parent);
		convert(viewHolder, getItem(position), position);
		System.out.println(position);
		return viewHolder.getConvertView();
	}
	
	public abstract void convert(ViewHolder viewHolder, T item, int position);
	
	private ViewHolder getViewHolder(View convertView, ViewGroup parent){
		return ViewHolder.get(context, convertView, parent, itemLayoutID);
	}

	public void setDataList(List<T> dataList) {
		if (this.dataList != null) {
			this.dataList.clear();
			this.dataList.addAll(dataList);
		} else {
			this.dataList = dataList;
		}
	}

	public List<T> getDataList() {
		return dataList;
	}
}
