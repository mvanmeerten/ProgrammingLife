package programminglife.model;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Throwing;
import programminglife.model.exception.UnknownTypeException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

/**
 * Created by marti_000 on 25-4-2017.
 */
public class Graph {
    private static final boolean PARSE_LINE_VERBOSE_DEFAULT = false;

    private String id;
    private Set<Node> rootNodes;

    /**
     * A list of nodes ordered by ID. Assumption: Nodes appear in GFA file in sequential order.
     */
    private Map<Integer, Node> nodes;

    public Graph(String id) {
        this.nodes = new HashMap<>();
        this.id = id;
        this.rootNodes = new HashSet<>();
    }

    public Node addNode(Node node) {
        return this.nodes.put(node.getId(), node);
    }

    public Node getNode(int id) {
        if (this.nodes.containsKey(id)) {
            return this.nodes.get(id);
        } else {
            throw new NoSuchElementException("There is no node with ID " + id);
        }
    }

    public Collection<Node> getNodes() {
        return this.nodes.values();
    }

    /**
     * Parse a {@link String} representing a {@link Node}.
     * @param propertyString the {@link String} from a GFA file.
     */
    private void parseSegment(String propertyString) {
        Node parsedNode = Node.parseSegment(propertyString);
        Node existingNode;
        try {
            existingNode = this.getNode(parsedNode.getId());
            existingNode.setSequence(parsedNode.getSequence());
        } catch (NoSuchElementException e) {
            this.addNode(parsedNode);
        }
    }

    /**
     * Parse a {@link String} representing a Link.
     * @param propertyString the {@link String} from a GFA file.
     */
    void parseLink(String propertyString) {
        String[] properties = propertyString.split("\\s");
        assert (properties[0].equals("L")); // properties[0] is 'L'
        int sourceId = Integer.parseInt(properties[1]);
        // properties[2] is unused
        int destinationId = Integer.parseInt(properties[3]);
        // properties[4] and further are unused

        Node sourceNode, destinationNode;

        try {
            sourceNode = this.getNode(sourceId);
        } catch (NoSuchElementException e) {
            sourceNode = new Node(sourceId);
            this.addNode(sourceNode);
        }

        try {
            destinationNode = this.getNode(destinationId);
        } catch (NoSuchElementException e) {
            destinationNode = new Node(destinationId);
            this.addNode(destinationNode);
        }

        sourceNode.addChild(destinationNode);
        destinationNode.addParent(sourceNode);
    }

    /**
     * Parse a GFA file as a {@link Graph}.
     * @param file the path to the GFA file.
     * @return the {@link Graph} object.
     * @throws FileNotFoundException when no file is found at the given path.
     * @throws UnknownTypeException when an unknown identifier (H/S/L) is read from the file.
     */
    public static Graph parse(String file) throws FileNotFoundException, UnknownTypeException {
        return parse(file, PARSE_LINE_VERBOSE_DEFAULT);
    }

    /**
     * Parse a GFA file as a {@link Graph}.
     * @param file the path to the GFA file.
     * @param verbose if log messages should be printed.
     * @return the {@link Graph} object.
     * @throws FileNotFoundException when no file is found at the given path.
     * @throws UnknownTypeException when an unknown identifier (H/S/L) is read from the file.
     */
    public static Graph parse(String file, boolean verbose) throws FileNotFoundException, UnknownTypeException {
        if (verbose) {
            System.out.println(String.format("Parsing file %s", file));
        }

        BufferedReader reader = new BufferedReader(new FileReader(file));
        Graph graph = new Graph(null);

        try {
            reader.lines().forEach(Errors.rethrow().wrap((Throwing.Consumer<String>) line -> {
                char type = line.charAt(0);

                switch (type) {
                    case 'S':
                        // Parse segment
                        graph.parseSegment(line);
                        break;
                    case 'L':
                        // Parse link
                        graph.parseLink(line);
                        break;
                    case 'H':
                        System.out.println(line);
                        break;
                    default:
                        // Otherwise
                        throw new UnknownTypeException(String.format("Unknown symbol '%c'", type));
                }
            }));
        } catch (Errors.WrappedAsRuntimeException e) {
            if (e.getCause() instanceof UnknownTypeException) {
                throw (UnknownTypeException) e.getCause();
            } else {
                throw e;
            }
        }

        for (Node n : graph.nodes.values()) {
            if (n != null && n.getParents().isEmpty()) {
                graph.rootNodes.add(n);
                if (verbose) {
                    System.out.println(String.format("Root node: %s", n));
                }
            }
        }

        return graph;
    }
}
