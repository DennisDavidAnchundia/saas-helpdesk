package com.helpdesk.dto;

import com.helpdesk.model.Message;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sobre de paginacion para el historial de chat.
 * La pagina 0 contiene los mensajes MAS RECIENTES; dentro de cada pagina
 * el contenido viene en orden cronologico (ASC) para mostrarlo tal cual.
 */
public class ChatPageResponse {

    private List<ChatMessageResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public ChatPageResponse() {}

    public static ChatPageResponse of(Page<Message> result) {
        ChatPageResponse r = new ChatPageResponse();
        List<ChatMessageResponse> asc = new ArrayList<>();
        for (Message m : result.getContent()) {
            asc.add(ChatMessageResponse.from(m));
        }
        Collections.reverse(asc);
        r.content = asc;
        r.page = result.getNumber();
        r.size = result.getSize();
        r.totalElements = result.getTotalElements();
        r.totalPages = result.getTotalPages();
        return r;
    }

    public List<ChatMessageResponse> getContent() { return content; }
    public void setContent(List<ChatMessageResponse> content) { this.content = content; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}
