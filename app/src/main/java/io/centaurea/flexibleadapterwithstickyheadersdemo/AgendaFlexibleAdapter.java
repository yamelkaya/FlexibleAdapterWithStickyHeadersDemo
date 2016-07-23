package io.centaurea.flexibleadapterwithstickyheadersdemo;

import android.support.annotation.NonNull;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.joda.time.DateTime;
import org.joda.time.Days;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;

public class AgendaFlexibleAdapter extends FlexibleAdapter{
    public static final int BATCH_LIMIT = 500;
    public static final int BATCH_LIMIT_CENTER = BATCH_LIMIT/2;
    private TaskDbProvider taskProvider;
    AgendaListItemComparator comparator = new AgendaListItemComparator();

    public AgendaFlexibleAdapter(TaskDbProvider taskProvider) {
        this(new ArrayList<>(),taskProvider);
    }

    private AgendaFlexibleAdapter(@NonNull List<AgendaListItemVM> items, TaskDbProvider taskProvider) {
        super(items);
        setDisplayHeadersAtStartUp(true);
        enableStickyHeaders();

        this.taskProvider = taskProvider;
    }

    public void loadItems(DateTime batchStartDate){
        removeAll();

        loadItemsFromDate(batchStartDate, BATCH_LIMIT_CENTER, true);
        loadItemsFromDate(batchStartDate, -BATCH_LIMIT_CENTER, false);
    }

    public int getGlobalPositionOfHeader(DateTime date) {
        int position = getHeaderPositionByDate(date);
        return position != -1 ? position : getNearestHeaderPositionByDate(date);
    }

    private int getNearestHeaderPositionByDate(DateTime date) {
        AgendaListHeaderVM lastHeaderDate = getLastHeader();
        AgendaListHeaderVM firstHeaderDate = getFirstHeader();

        if (lastHeaderDate != null && firstHeaderDate != null){
            int diffInDaysLast = Math.abs(Days.daysBetween(date,lastHeaderDate.getDateTime()).getDays());
            int diffInDaysFirst = Math.abs(Days.daysBetween(date,firstHeaderDate.getDateTime()).getDays());

            return diffInDaysFirst < diffInDaysLast ? getGlobalPositionOf(firstHeaderDate) : getGlobalPositionOf(lastHeaderDate);
        }
        else return -1;
    }

    private int getHeaderPositionByDate(DateTime date){
        AgendaListHeaderVM header = getHeaderByDate(date);
        return header != null ? getGlobalPositionOf(header) : -1;
    }

    private AgendaListHeaderVM getHeaderByDate(DateTime date) {
        AgendaListHeaderVM header = null;
        DateTime startOfDate = date.withTimeAtStartOfDay();
        List<AgendaListHeaderVM> tasksScheduledToDate = (List<AgendaListHeaderVM>) Stream.of(getHeaderItems()).filter(i -> ((AgendaListHeaderVM) i).getDateTime().equals(startOfDate)).collect(Collectors.toList());

        if (tasksScheduledToDate.size() > 0) {
            header = tasksScheduledToDate.get(0);
        }
        return header;
    }

    private List<AgendaListItemVM> generateAgendaListItems(List<Task> tasks){
        List<AgendaListItemVM> data = new ArrayList<>();
        Map<String,AgendaListHeaderVM> headers = new HashMap<>();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        for (int i = 0; i < tasks.size(); i++) {
            Task item = tasks.get(i);
            String dateKey = format.format(item.getStartTime());

            AgendaListHeaderVM header = getHeaderByDate(new DateTime(item.getStartTime()));
            if (header == null) {
                header = headers.get(dateKey);

                if (header == null) {
                    header = new AgendaListHeaderVM(item.getStartTime(),false);
                    headers.put(dateKey, header);
                }
            }

            data.add(new AgendaListItemVM(header, item, false));
        }

        return data;
    }

    private void loadItemsFromDate(DateTime date, int offset, boolean equal){
        List<Task> tasksPage = taskProvider.getTasksPage(date, offset, equal);

        updateItems(tasksPage, offset);
        insertEmptySections();
    }

    private void insertEmptySections() {
        List<AgendaListHeaderVM> headers = getHeaderItems();
        for (int i=0; i<headers.size() - 1; i++){
            AgendaListHeaderVM header = headers.get(i);
            AgendaListHeaderVM nextHeader = headers.get(i + 1);

            int diffInDays = Days.daysBetween(header.getDateTime(),nextHeader.getDateTime()).getDays();

            for (int j=1; j<diffInDays; j++){
                //TODO: add empty item is a workaround - actually we need just empty section.
                addItem(AgendaListItemVM.getEmpty(header.getDateTime().plusDays(j)));
                //addSection(new AgendaListHeaderVM(header.getDateTime().plusDays(j).toDate(),true),comparator);
            }
        }
    }

    private void updateItems(List<Task> tasksPage, int offset) {
        List<AgendaListItemVM> newItems = generateAgendaListItems(tasksPage);

        if (getItemCount() > 0){
            addItems(newItems);
        }
        else{
            addItems(0,newItems);
        }
    }

    private AgendaListHeaderVM getLastHeader(){
        return ((AgendaListHeaderVM)getSectionHeader(getItemCount() - 1));
    }

    private AgendaListHeaderVM getFirstHeader(){
        return ((AgendaListHeaderVM)getSectionHeader(0));
    }

    private void removeAll(){
        removeRange(0,getItemCount());
    }

    private void addItems(List<AgendaListItemVM> items){
        for (AgendaListItemVM item : items){
            addItem(item);
        }
    }

    private void addItem(AgendaListItemVM item){
        addItem(calculatePositionFor(item,comparator),item);
    }

    private class AgendaListItemComparator implements Comparator<IFlexible> {

        @Override
        public int compare(IFlexible v1, IFlexible v2) {
            int result = 0;
            if (v1 instanceof AgendaListHeaderVM && v2 instanceof AgendaListHeaderVM) {
                result = ((AgendaListHeaderVM) v1).getDateTime().compareTo(((AgendaListHeaderVM) v2).getDateTime());

            } else if (v1 instanceof AgendaListItemVM && v2 instanceof AgendaListItemVM) {
                result = ((AgendaListItemVM) v1).getHeader().getDateTime().compareTo(((AgendaListItemVM) v2).getHeader().getDateTime());
                if (result == 0)
                    result = ((AgendaListItemVM) v1).getDateTime().compareTo(((AgendaListItemVM) v2).getDateTime());

            } else if (v1 instanceof AgendaListItemVM && v2 instanceof AgendaListHeaderVM) {
                result = ((AgendaListItemVM) v1).getHeader().getDateTime().compareTo(((AgendaListHeaderVM) v2).getDateTime());
                if (result == 0) result--;

            } else if (v1 instanceof AgendaListHeaderVM && v2 instanceof AgendaListItemVM) {
                result = ((AgendaListHeaderVM) v1).getDateTime().compareTo(((AgendaListItemVM) v2).getHeader().getDateTime());
                if (result == 0) result--;
            }
            return result;
        }
    }
}
