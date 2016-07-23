package io.centaurea.flexibleadapterwithstickyheadersdemo;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.databinding.library.baseAdapters.BR;

import org.joda.time.DateTime;

import java.util.List;
import java.util.UUID;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractSectionableItem;
import eu.davidea.viewholders.FlexibleViewHolder;

public class AgendaListItemVM extends AbstractSectionableItem<AgendaListItemVM.ItemViewHolder, AgendaListHeaderVM> {

    private final String title;
    private final String id;
    private final boolean empty;

    private DateTime startDate;

    public String getTitle() {
        return title;
    }

    public DateTime getDateTime() { return startDate;}

    //TODO:REF refactor to using RecycleView.ViewHolder instead of FlexibleViewHolder when FlexibleAdapter release will be updated
    public static class ItemViewHolder extends FlexibleViewHolder {

        private final ViewDataBinding _binding;

        public ItemViewHolder(View itemView, FlexibleAdapter adapter) {
            super(itemView, adapter);
            _binding = DataBindingUtil.bind(itemView);
        }
    }

    public AgendaListItemVM(AgendaListHeaderVM header, Task dataItem, boolean empty) {
        super(header);
        this.id = dataItem.get_id();
        this.title = dataItem.getTitle();
        this.startDate = new DateTime(dataItem.getStartTime());
        this.empty = empty;
    }

    public boolean isScheduledToDate(DateTime date){
        DateTime dayStart =  new DateTime(date).withTimeAtStartOfDay();
        DateTime dayEnd = dayStart.plusDays(1).minusMillis(1);

        return startDate.isAfter(dayStart) && startDate.isBefore(dayEnd);
    }

    @Override
    public int getLayoutRes() {
        return this.empty ? R.layout.view_agenda_empty_item : R.layout.view_agenda_item;
        //return R.layout.view_agenda_item;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof AgendaListItemVM){
            return this.id.equals(((AgendaListItemVM) o).id);
        }
        return false;
    }

    @Override
    public ItemViewHolder createViewHolder(FlexibleAdapter adapter, LayoutInflater inflater, ViewGroup parent) {
        ViewDataBinding binding = DataBindingUtil.inflate(inflater, getLayoutRes(), parent, false);
        return new ItemViewHolder(binding.getRoot(), adapter);

    }

    @Override
    public void bindViewHolder(FlexibleAdapter adapter, ItemViewHolder holder, int position, List payloads) {
        holder._binding.setVariable(BR.item,this);
    }

    public static AgendaListItemVM getEmpty(DateTime date){
        AgendaListHeaderVM newHeader = new AgendaListHeaderVM(date.toDate(), true);
        Task task = new Task();
        task.setStartTime(newHeader.getDateTime().plusHours(2).toDate());
        task.setTitle("empty");
        task.set_id(UUID.randomUUID().toString());

        return new AgendaListItemVM(newHeader,task,true);
    }
}
