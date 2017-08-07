package com.gentics.mesh.core.data.page.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.gentics.ferma.Tx;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.TransformableElement;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.parameter.PagingParameters;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

/**
 * This page implementation will handle paging internally and on-demand. The internal paging will only iterate over as many items as the needed operation
 * requires. Loading the first page will thus only iterate over the elements of the first page. Loading the total count on the other hand requires the
 * implementation to iterate over all edges.
 * 
 * @param <T>
 */
public class DynamicTransformablePageImpl<T extends TransformableElement<? extends RestModel>> implements TransformablePage<T> {

	private long pageNumber;

	private int perPage;

	private InternalActionContext ac;

	private Long totalPages = null;

	private List<T> elementsOfPage = new ArrayList<>();

	private AtomicLong totalCounter = new AtomicLong();

	/**
	 * Iterator over all items.
	 */
	private Iterator<? extends Vertex> visibleItems;

	private AtomicBoolean pageFull = new AtomicBoolean(false);

	private AtomicBoolean hasNextPage = new AtomicBoolean();

	private long lowerBound;

	private GraphPermission perm;

	private Predicate<Vertex> extraFilter;

	private DynamicTransformablePageImpl(InternalActionContext ac, PagingParameters pagingInfo, GraphPermission perm, Predicate<Vertex> extraFilter) {
		if (pagingInfo.getPage() < 1) {
			throw new GenericRestException(BAD_REQUEST, "error_page_parameter_must_be_positive", String.valueOf(pagingInfo.getPage()));
		}
		if (pagingInfo.getPerPage() < 0) {
			throw new GenericRestException(BAD_REQUEST, "error_pagesize_parameter", String.valueOf(pagingInfo.getPerPage()));
		}
		this.pageNumber = pagingInfo.getPage();
		this.perPage = pagingInfo.getPerPage();
		this.ac = ac;
		this.perm = perm;
		this.extraFilter = extraFilter;

		this.lowerBound = (pageNumber - 1) * perPage;

		if (perPage == 0) {
			this.lowerBound = 0;
		}

	}

	/**
	 * Create new dynamic page.
	 * 
	 * @param ac
	 *            Context of the operation
	 * @param root
	 *            Root vertex which provides the elements which can be paged
	 * @param pagingInfo
	 *            Paging information which contains the perPage and page information
	 */
	public DynamicTransformablePageImpl(InternalActionContext ac, RootVertex<? extends T> root, PagingParameters pagingInfo) {
		this(ac, root, pagingInfo, READ_PERM, null);
	}

	/**
	 * Create a new dynamic page.
	 * 
	 * @param ac
	 *            Context of the operation
	 * @param root
	 *            Root vertex which provides the elements which can be paged
	 * @param pagingInfo
	 *            Paging information which contains the perPage and page information
	 * @param perm
	 *            Permission used to filter elements by
	 * @param extraFilter
	 *            Optional extra filter to filter by
	 * 
	 */
	public DynamicTransformablePageImpl(InternalActionContext ac, RootVertex<? extends T> root, PagingParameters pagingInfo, GraphPermission perm,
			Predicate<Vertex> extraFilter) {
		this(ac, pagingInfo, perm, extraFilter);
		init(root.getPersistanceClass(), "e." + root.getRootLabel().toLowerCase() + "_out", root.getId(), Direction.IN, root.getGraph());
	}

	/**
	 * Create a new dynamic page.
	 * 
	 * @param ac
	 *            Current context
	 * @param indexName
	 *            Name of the index which should be used to lookup the elements
	 * @param indexKey
	 *            Key to be used for the index lookup
	 * @param clazz
	 *            Class of the element to be returned
	 * @param pagingInfo
	 *            Paging parameters
	 */
	public DynamicTransformablePageImpl(InternalActionContext ac, String indexName, Object indexKey, Class<T> clazz, PagingParameters pagingInfo,
			GraphPermission perm, Predicate<Vertex> extraFilter) {
		this(ac, pagingInfo, perm, extraFilter);
		init(clazz, indexName, indexKey, Direction.OUT, Tx.getActive().getGraph());
	}

	/**
	 * Initalize the dynamic iterator which is bound to the most getters of this class. A stream is setup which is used to filter out the unwanted data. Paging
	 * is also handled via the stream. At the end only a iterator is provided for the other methods. The iterator next method is invoked until the needed data
	 * is provided or the iterator has no more items.
	 * 
	 * @param clazz
	 *            Class used to frame the found elements.
	 * @param indexName
	 *            Name of the graph index to use
	 * @param indexKey
	 *            Key object used for the lookup
	 * @param vertexDirection
	 *            The direction to be resolved for each resulting edge in order to get to the target element.
	 * @param graph
	 *            Framed graph used to re-frame the resulting elements
	 */
	private void init(Class<? extends T> clazz, String indexName, Object indexKey, Direction vertexDirection, FramedGraph graph) {
		MeshAuthUser requestUser = ac.getUser();

		// Iterate over all vertices that are managed by this root vertex
		Spliterator<Edge> itemEdges = graph.getEdges(indexName, indexKey).spliterator();
		AtomicLong pageCounter = new AtomicLong();

		Stream<Vertex> stream = StreamSupport.stream(itemEdges, false)

				// Sort the elements by element id
				// .sorted(new ElementIdComparator())

				// Get the vertex from the edge
				.map(itemEdge -> {
					return itemEdge.getVertex(vertexDirection);
				})

				// Only handle elements which are visible to the user
				.filter(item -> requestUser.hasPermissionForId(item.getId(), perm));

		if (extraFilter != null) {
			stream = stream.filter(extraFilter);
		}

		visibleItems = stream.map(item -> {
			totalCounter.incrementAndGet();
			return item;
		})

				// Apply paging - skip to lower bounds
				.skip(lowerBound)

				.map(item -> {
					// Only add elements to the list if those elements are part of selected the page
					long elementsInPage = pageCounter.get();
					if (elementsInPage < perPage) {
						elementsOfPage.add(graph.frameElementExplicit(item, clazz));
						pageCounter.incrementAndGet();
					} else {
						pageFull.set(true);
						hasNextPage.set(true);
					}
					return item;
				})

				.iterator();

	}

	@Override
	public int getPerPage() {
		return perPage;
	}

	@Override
	public long getPageCount() {
		if (totalPages == null) {
			// The totalPages of the list response must be zero if the perPage parameter is also zero.
			totalPages = 0L;
			if (perPage != 0) {
				totalPages = (long) Math.ceil(getTotalElements() / (double) (perPage));
			}
		}
		return totalPages;
	}

	@Override
	public long getNumber() {
		return pageNumber;
	}

	@Override
	public long getTotalElements() {
		while (visibleItems.hasNext()) {
			visibleItems.next();
		}
		return totalCounter.get();
	}

	@Override
	public int getSize() {
		return getWrappedList().size();
	}

	@Override
	public List<? extends T> getWrappedList() {
		// Iterate over more edges if the page is not yet full and there are any more edges
		while (visibleItems.hasNext() && !pageFull.get()) {
			visibleItems.next();
		}
		return elementsOfPage;
	}

	@Override
	public boolean hasNextPage() {
		// Iterate over more items as long as the hasNextPage flag has not been set
		while (!hasNextPage.get() && visibleItems.hasNext()) {
			visibleItems.next();
		}
		return hasNextPage.get();
	}

}