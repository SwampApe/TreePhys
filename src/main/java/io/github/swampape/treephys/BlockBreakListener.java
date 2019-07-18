package io.github.swampape.treephys;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public final class BlockBreakListener implements Listener {

    private static Material[] MATERIAL_LOGS = {Material.ACACIA_LOG, Material.BIRCH_LOG, Material.DARK_OAK_LOG, Material.JUNGLE_LOG, Material.OAK_LOG, Material.SPRUCE_LOG};
    private static Material[] MATERIAL_LEAVES = {Material.ACACIA_LEAVES, Material.BIRCH_LEAVES, Material.DARK_OAK_LEAVES, Material.JUNGLE_LEAVES, Material.OAK_LEAVES, Material.SPRUCE_LEAVES};



    /*
        Check if tree
        Check above for wood blocks and diagonally
        If leaves around any block(leaf must match log)
     */
    private boolean isTree(Block block) {

        return true;
    }

    /*
        Check which blocks to break
          Check above and diagonally for matching wood blocks
          Check if "supported"
              Check all wood blocks under it(diagonal or under)
              Check if those wood blocks is supported
              Supported if block under is dirt
              If no logs above mark as leaves center


        Check leaves to break
          Must be above initially broken block
          Check above and below for connected leaves
          Leaves must match type of wood
          Centered around "leaves centers"
          Smallest radius search
              Search radially until block other than leaves are hit
              If at a corner use radius otherwise radius-1

    */
    private Material getLeafType(Material block) {
        for(int i = 0; i < MATERIAL_LOGS.length; i++) {
            if (block.equals(MATERIAL_LOGS[i])) {
                return MATERIAL_LEAVES[i];
            }
        }
        return null;
    }


    private ArrayList<Block> checkMatchingBlockType(Block checkAgainst, Block...toCheck) {
        return checkMatchingBlockType(checkAgainst.getType(), toCheck);
    }

    private ArrayList<Block> checkMatchingBlockType(Material checkAgainst, Block...toCheck) {
        ArrayList<Block> matching = new ArrayList<Block>();
        for(Block block : toCheck) {
            if(block.getType().equals(checkAgainst)) {
                matching.add(block);
            }
        }
        return matching;
    }

    private boolean checkSupported(Block initial, Tree tree) {
        Block down = initial.getRelative(0, -1, 0);
        if(down.getType().equals(Material.DIRT) || down.getType().equals(Material.GRASS_BLOCK)) {
            tree.supportedLogs.add(initial);
            return true;
        }
        Block north = initial.getRelative(0, 0, -1);
        Block west = initial.getRelative(-1, 0, 0);
        Block south = initial.getRelative(0, 0, 1);
        Block east = initial.getRelative(1, 0, 0);
        Block diagNorth = initial.getRelative(0, -1, -1);
        Block diagWest = initial.getRelative(-1, -1, 0);
        Block diagSouth = initial.getRelative(0, -1, 1);
        Block diagEast = initial.getRelative(1, -1, 0);
        ArrayList<Block> matching = checkMatchingBlockType(initial, down, diagNorth, diagWest, diagSouth, diagEast, north, west, south, east);

        for(Block block : tree.supportedLogs) {
            for(int i = 0; i < matching.size(); i++) {
                if(matching.get(i).equals(block)) {
                    return true;
                }
            }
        }
        for(Block block : tree.checkedLogs) {
            for(int i = 0; i < matching.size(); i++) {
                if(matching.get(i).equals(block)) {
                    matching.remove(i);
                }
            }
        }

        tree.checkedLogs.add(initial);
        for(int i = 0; i < matching.size(); i++) {
            if(checkSupported(matching.get(i), tree)) {
                tree.checkedLogs.remove(tree.checkedLogs.size() - 1);
                tree.supportedLogs.add(initial);
                return true;
            }
        }
        tree.unsupportedLogs.add(initial);
        return false;
    }

    private void searchIfNotSupported(Block initial, Tree tree) {
        if(!checkSupported(initial,  tree)) {
            searchDependentBranches(initial, tree);
        }
    }

    private void searchDependentBranches(Block initial, Tree tree) {
        tree.unsupportedLogs.add(initial);
        tree.checkedLogs.add(initial);
        Block up = initial.getRelative(0, 1, 0);
        Block diagNorth = initial.getRelative(0, 1, -1);
        Block diagWest = initial.getRelative(-1, 1, 0);
        Block diagSouth = initial.getRelative(0, 1, 1);
        Block diagEast = initial.getRelative(1, 1, 0);
        Block northWest = initial.getRelative(-1, 0, -1);
        Block northEast = initial.getRelative(1, 0, -1);
        Block southWest = initial.getRelative(-1, 0, 1);
        Block southEast = initial.getRelative(1, 0, 1);
        ArrayList<Block> matching = checkMatchingBlockType(initial, up, diagNorth, diagWest, diagSouth, diagEast, northWest, northEast, southWest, southEast);
        for(Block block : tree.checkedLogs) {
            for(int i = 0; i < matching.size(); i++) {
                if(matching.get(i).equals(block)) {
                    matching.remove(i);
                }
            }
        }
        if(matching.isEmpty()) {
            tree.leavesCenters.add(initial);
        } else {
            for(int i = 0; i < matching.size(); i++) {
                searchIfNotSupported(matching.get(i), tree);
            }
        }
    }

    /*
    Tree characteristics
    Oak - 2
    Acacia - 3
    Jungle - 5
    Spruce - 3
    Dark Oak - 3
    Birch - 2

    Check some branches out
    Check for other trees

     */
    private void searchLeaves(Tree tree) {
        if(tree.leavesCenters.size() == 0) {
            return;
        }
        Material leafType = getLeafType(tree.leavesCenters.get(0).getType());
        for(Block center : tree.leavesCenters) {
            int upperLimit = 0;
            int bottomLimit = 0;
            while(true) {
                Block north = center.getRelative(0, bottomLimit, -1);
                Block west = center.getRelative(-1, bottomLimit, 0);
                Block south = center.getRelative(0, bottomLimit, 1);
                Block east = center.getRelative(1, bottomLimit, 0);
                ArrayList<Block> matching = checkMatchingBlockType(leafType, north, west, south, east);
                if(matching.size() < 2) {
                    ++bottomLimit;
                    break;
                }
                --bottomLimit;
            }
            int lowest = tree.unsupportedLogs.get(0).getLocation().getBlockY();
            int centerHeight = center.getLocation().getBlockY();
            if(centerHeight + bottomLimit < lowest) {
                bottomLimit = lowest - centerHeight;
            }
            while(true) {
                Block north = center.getRelative(0, upperLimit, -1);
                Block west = center.getRelative(-1, upperLimit, 0);
                Block south = center.getRelative(0, upperLimit, 1);
                Block east = center.getRelative(1, upperLimit, 0);
                ArrayList<Block> matching = checkMatchingBlockType(leafType, north, west, south, east);
                if(matching.size() < 2) {
                    --upperLimit;
                    break;
                }
                ++upperLimit;
            }

            for(int i = bottomLimit; i <= upperLimit;) {
                boolean hitBlock = false;
                int size = 1;
                while(true) {
                    for (int j = -(size - 1); j <= size - 1; j++) {
                        Block west = center.getRelative(j, i, size);
                        Block east = center.getRelative(j, i, -size);
                        if (checkMatchingBlockType(leafType, west, east).size() != 2) {
                            --size;
                            hitBlock = true;
                            break;
                        }
                    }
                    if (!hitBlock) {
                        for (int j = -size; j <= size; j++) {
                            Block north = center.getRelative(size, i, j);
                            Block south = center.getRelative(-size, i, j);
                            if (checkMatchingBlockType(leafType, north, south).size() != 2) {
                                hitBlock = true;
                                break;
                            }
                        }
                    }

                    if (hitBlock) {
                        for (int k = 0; k <= size; ++k) {
                            for (int j = -(k - 1); j <= k - 1; j++) {
                                Block west = center.getRelative(j, i, k);
                                Block east = center.getRelative(j, i, -k);
                                tree.treeLeaves.add(west);
                                tree.treeLeaves.add(east);
                            }
                            for (int j = -k; j <= k; j++) {
                                Block north = center.getRelative(k, i, j);
                                Block south = center.getRelative(-k, i, j);
                                tree.treeLeaves.add(north);
                                tree.treeLeaves.add(south);
                            }
                        }
                        ++i;
                        break;
                    }
                    ++size;
                }
            }
        }
    }

    private void breakTree(Tree tree, ItemStack item) {
        for(Block block : tree.unsupportedLogs) {
            block.breakNaturally(item);
        }
        for(Block block : tree.treeLeaves) {
            block.breakNaturally();
        }
    }

    private void handlePotentialTree(Block block, ItemStack item) {
        if(isTree(block)) {
            Tree tree = new Tree();
            searchDependentBranches(block, tree);
            searchLeaves(tree);
            breakTree(tree, item);
        }
    }


    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreak(BlockBreakEvent event) {
        if(!event.isCancelled()) {
            for(Material block : MATERIAL_LOGS) {
                if(event.getBlock().getType().equals(block)) {
                    handlePotentialTree(event.getBlock(), event.getPlayer().getInventory().getItemInMainHand());
                }
            }
        }
    }


    private static class Tree {
        public ArrayList<Block> leavesCenters = new ArrayList<Block>();
        public ArrayList<Block> unsupportedLogs = new ArrayList<Block>();
        public ArrayList<Block> supportedLogs = new ArrayList<Block>();
        public ArrayList<Block> checkedLogs = new ArrayList<Block>();
        public ArrayList<Block> treeLeaves = new ArrayList<Block>();
    }
}
