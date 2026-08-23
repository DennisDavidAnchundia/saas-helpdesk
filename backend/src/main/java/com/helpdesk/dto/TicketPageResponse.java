package com.helpdesk.dto;

import com.helpdesk.model.Ticket;
import org.springframework.data.domain.Page;

import java.util.List;

/** Sobre de paginacion estandar para listados de tickets. */
public class TicketPageResponse {

    private List<TicketResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public TicketPageResponse() {}

    public static TicketPageResponse of(Page<Ticket> result) {
        TicketPageResponse r = new TicketPageResponse();
        r.content = result.getContent().stream().map(TicketResponse::from).toList();
        r.page = result.getNumber();
        r.size = result.getSize();
        r.totalElements = result.getTotalElements();
        r.totalPages = result.getTotalPages();
        return r;
    }

    public List<TicketResponse> getContent() { return content; }
    public void setContent(List<TicketResponse> content) { this.content = content; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}
