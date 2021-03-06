package net.ocheyedan.ply.cmd.build;

import net.ocheyedan.ply.graph.DirectedAcyclicGraph;
import net.ocheyedan.ply.props.*;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.*;
import static net.ocheyedan.ply.props.PropFile.Prop;

/**
 * User: blangel
 * Date: 1/2/12
 * Time: 5:36 PM
 */
public class AliasTest {

    @Test public void parseAlias() {
        Alias.Resolver resolver = new Alias.Resolver(new File("./ply/config"));
        // test alias, no expansion
        String name = "clean", value = "\"rm -rf target\"";
        Map<String, Prop> unparsedAliases = new HashMap<String, Prop>();
        PropFile container = new PropFile(Context.named("aliases"), PropFile.Loc.Local);
        unparsedAliases.put(name, container.add(name, value));
        Alias alias = resolver.parseAlias(Scope.Default, Script.parse(name, Scope.Default), value, unparsedAliases,
                                       new DirectedAcyclicGraph<String>(), new HashMap<String, Alias>(), new ArrayList<String>());
        assertEquals("clean", alias.name);
        assertEquals("clean", alias.unparsedName);
        assertEquals(1, alias.scripts.size());
        assertEquals("rm", alias.scripts.get(0).name);
        assertEquals(2, alias.scripts.get(0).arguments.size());
        assertEquals("-rf", alias.scripts.get(0).arguments.get(0));
        assertEquals("target", alias.scripts.get(0).arguments.get(1));
        // test circular reference
        name = "clean";
        value = "\"rm -rf target\" clean";
        unparsedAliases.clear();
        container = new PropFile(Context.named("aliases"), PropFile.Loc.Local);
        unparsedAliases.put(name, container.add(name, value));
        try {
            resolver.parseAlias(Scope.Default, Script.parse(name, Scope.Default), value, unparsedAliases,
                    new DirectedAcyclicGraph<String>(), new HashMap<String, Alias>(), new ArrayList<String>());
            fail("Expected a circular reference exception");
        } catch (Alias.CircularReference cr) {
            // expected
        }
        // test alias expansion
        name = "clean";
        value = "\"rm -rf target\"";
        unparsedAliases.clear();
        container = new PropFile(Context.named("aliases"), PropFile.Loc.Local);
        unparsedAliases.put(name, container.add(name, value));
        name = "compile";
        value = "clean compiler.jar";
        unparsedAliases.put(name, container.add(name, value));
        alias = resolver.parseAlias(Scope.Default, Script.parse(name, Scope.Default), value, unparsedAliases,
                                 new DirectedAcyclicGraph<String>(), new HashMap<String, Alias>(), new ArrayList<String>());
        assertEquals("compile", alias.name);
        assertEquals("compile", alias.unparsedName);
        assertEquals(2, alias.scripts.size());
        assertEquals("clean", alias.scripts.get(0).name);
        assertEquals("clean", alias.scripts.get(0).unparsedName);
        assertTrue(alias.scripts.get(0).getClass() == Alias.class);
        assertEquals(1, ((Alias) alias.scripts.get(0)).scripts.size());
        assertEquals("rm", ((Alias) alias.scripts.get(0)).scripts.get(0).name);
        assertEquals(2, ((Alias) alias.scripts.get(0)).scripts.get(0).arguments.size());
        assertEquals("-rf", ((Alias) alias.scripts.get(0)).scripts.get(0).arguments.get(0));
        assertEquals("target", ((Alias) alias.scripts.get(0)).scripts.get(0).arguments.get(1));
        assertEquals("rm -rf target", ((Alias) alias.scripts.get(0)).scripts.get(0).unparsedName);
        assertEquals("compiler.jar", alias.scripts.get(1).name);
        assertEquals("compiler.jar", alias.scripts.get(1).unparsedName);
        assertTrue(alias.scripts.get(1).getClass() == Script.class);
        // augment clean for double-alias expansion
        name = "clean";
        value = "\"rm -rf target\" remove";
        container = new PropFile(Context.named("aliases"), PropFile.Loc.Local);
        unparsedAliases.put(name, container.add(name, value));
        name = "remove";
        value = "remove-1.0.jar";
        unparsedAliases.put(name, container.add(name, value));
        name = "compile";
        value = "clean compiler.jar";
        alias = resolver.parseAlias(Scope.Default, Script.parse(name, Scope.Default), value, unparsedAliases,
                                 new DirectedAcyclicGraph<String>(), new HashMap<String, Alias>(), new ArrayList<String>());
        assertEquals("compile", alias.name);
        assertEquals("compile", alias.unparsedName);
        assertEquals(2, alias.scripts.size());
        assertEquals("clean", alias.scripts.get(0).name);
        assertEquals("clean", alias.scripts.get(0).unparsedName);
        assertTrue(alias.scripts.get(0).getClass() == Alias.class);
        assertEquals(2, ((Alias) alias.scripts.get(0)).scripts.size());
        assertEquals("rm", ((Alias) alias.scripts.get(0)).scripts.get(0).name);
        assertEquals(2, ((Alias) alias.scripts.get(0)).scripts.get(0).arguments.size());
        assertEquals("-rf", ((Alias) alias.scripts.get(0)).scripts.get(0).arguments.get(0));
        assertEquals("target", ((Alias) alias.scripts.get(0)).scripts.get(0).arguments.get(1));
        assertEquals("rm -rf target", ((Alias) alias.scripts.get(0)).scripts.get(0).unparsedName);
        assertTrue(((Alias) alias.scripts.get(0)).scripts.get(0).getClass() == Script.class);
        assertEquals("remove", ((Alias) alias.scripts.get(0)).scripts.get(1).name);
        assertEquals("remove", ((Alias) alias.scripts.get(0)).scripts.get(1).unparsedName);
        assertTrue(((Alias) alias.scripts.get(0)).scripts.get(1).getClass() == Alias.class);
        assertEquals(1, ((Alias) ((Alias) alias.scripts.get(0)).scripts.get(1)).scripts.size());
        assertEquals("remove-1.0.jar", ((Alias) ((Alias) alias.scripts.get(0)).scripts.get(1)).scripts.get(0).name);
        assertEquals("remove-1.0.jar", ((Alias) ((Alias) alias.scripts.get(0)).scripts.get(1)).scripts.get(0).unparsedName);
        assertTrue(((Alias) ((Alias) alias.scripts.get(0)).scripts.get(1)).scripts.get(0).getClass() == Script.class);
        assertEquals("compiler.jar", alias.scripts.get(1).name);
        assertEquals("compiler.jar", alias.scripts.get(1).unparsedName);
        assertTrue(alias.scripts.get(1).getClass() == Script.class);
        // circular exception from double-alias expansion
        name = "remove";
        value = "remove-1.0.jar clean";
        container = new PropFile(Context.named("aliases"), PropFile.Loc.Local);
        unparsedAliases.put(name, container.add(name, value));
        name = "compile";
        value = "clean compiler.jar";
        try {
            resolver.parseAlias(Scope.Default, Script.parse(name, Scope.Default), value, unparsedAliases,
                             new DirectedAcyclicGraph<String>(), new HashMap<String, Alias>(), new ArrayList<String>());
            fail("Expected a circular reference exception");
        } catch (Alias.CircularReference cr) {
            // expected
        }
        // pre-seed cache with 'test' scope for this test
        Map<String, Prop> testAliases = new HashMap<String, Prop>(1);
        PropFile testContainer = new PropFile(Context.named("aliases"), Scope.named("test"), PropFile.Loc.Local);
        testAliases.put("remove", testContainer.add("remove", "remove-1.0.jar"));
        resolver.mappedPropCache.put(Scope.named("test"), testAliases);
        // test alias/script mapped to different scope
        name = "clean";
        value = "\"rm -rf target\" test:remove";
        unparsedAliases.clear();
        container = new PropFile(Context.named("aliases"), PropFile.Loc.Local);
        unparsedAliases.put(name, container.add(name, value));
        name = "remove"; // since clean references this alias from a scope, needs to be part of ply's own aliases
        value = "remove-1.0.jar";
        unparsedAliases.put(name, container.add(name, value));
        name = "compile";
        value = "clean test:compiler.jar";
        unparsedAliases.put(name, container.add(name, value));
        alias = resolver.parseAlias(Scope.Default, Script.parse(name, Scope.Default), value, unparsedAliases,
                                 new DirectedAcyclicGraph<String>(), new HashMap<String, Alias>(), new ArrayList<String>());
        assertEquals("compile", alias.name);
        assertEquals("compile", alias.unparsedName);
        assertEquals(Scope.Default, alias.scope);
        assertEquals(2, alias.scripts.size());
        assertEquals("clean", alias.scripts.get(0).name);
        assertEquals("clean", alias.scripts.get(0).unparsedName);
        assertEquals(Scope.Default, alias.scripts.get(0).scope);
        assertTrue(alias.scripts.get(0).getClass() == Alias.class);
        assertEquals(2, ((Alias) alias.scripts.get(0)).scripts.size());
        assertEquals("rm", ((Alias) alias.scripts.get(0)).scripts.get(0).name);
        assertEquals(2, ((Alias) alias.scripts.get(0)).scripts.get(0).arguments.size());
        assertEquals("-rf", ((Alias) alias.scripts.get(0)).scripts.get(0).arguments.get(0));
        assertEquals("target", ((Alias) alias.scripts.get(0)).scripts.get(0).arguments.get(1));
        assertEquals("rm -rf target", ((Alias) alias.scripts.get(0)).scripts.get(0).unparsedName);
        assertEquals(Scope.Default, ((Alias) alias.scripts.get(0)).scripts.get(0).scope);
        assertTrue(((Alias) alias.scripts.get(0)).scripts.get(0).getClass() == Script.class);
        assertEquals("remove", ((Alias) alias.scripts.get(0)).scripts.get(1).name);
        assertEquals("test:remove", ((Alias) alias.scripts.get(0)).scripts.get(1).unparsedName);
        assertEquals(new Scope("test"), ((Alias) alias.scripts.get(0)).scripts.get(1).scope);
        assertTrue(((Alias) alias.scripts.get(0)).scripts.get(1).getClass() == Alias.class);
        assertEquals(1, ((Alias) ((Alias) alias.scripts.get(0)).scripts.get(1)).scripts.size());
        assertEquals("remove-1.0.jar", ((Alias) ((Alias) alias.scripts.get(0)).scripts.get(1)).scripts.get(0).name);
        assertEquals("remove-1.0.jar", ((Alias) ((Alias) alias.scripts.get(0)).scripts.get(1)).scripts.get(0).unparsedName);
        assertEquals(new Scope("test"), ((Alias) ((Alias) alias.scripts.get(0)).scripts.get(1)).scripts.get(0).scope);
        assertTrue(((Alias) ((Alias) alias.scripts.get(0)).scripts.get(1)).scripts.get(0).getClass() == Script.class);
        assertEquals("compiler.jar", alias.scripts.get(1).name);
        assertEquals("test:compiler.jar", alias.scripts.get(1).unparsedName);
        assertTrue(alias.scripts.get(1).getClass() == Script.class);
        assertEquals("test", alias.scripts.get(1).scope.name);
        // test where an alias maps to two aliases which themselves both map to the same alias (but non-cyclic) which
        // is a valid case
        name = "clean";
        value = "resolve clean-1.0.jar";
        unparsedAliases.clear();
        container = new PropFile(Context.named("aliases"), PropFile.Loc.Local);
        unparsedAliases.put(name, container.add(name, value));
        name = "resolve";
        value = "resolve-1.0.jar";
        unparsedAliases.put(name, container.add(name, value));
        name = "compile";
        value = "clean resolve compiler.jar";
        unparsedAliases.put(name, container.add(name, value));
        alias = resolver.parseAlias(Scope.Default, Script.parse(name, Scope.Default), value, unparsedAliases,
                                 new DirectedAcyclicGraph<String>(), new HashMap<String, Alias>(), new ArrayList<String>());
        assertEquals("compile", alias.name);
        assertEquals("compile", alias.unparsedName);
        assertEquals(Scope.Default, alias.scope);
        assertEquals(3, alias.scripts.size());
        assertEquals("clean", alias.scripts.get(0).name);
        assertEquals("clean", alias.scripts.get(0).unparsedName);
        assertEquals(Scope.Default, alias.scripts.get(0).scope);
        assertTrue(alias.scripts.get(0).getClass() == Alias.class);
        assertEquals(2, ((Alias) alias.scripts.get(0)).scripts.size());
        assertEquals("resolve", ((Alias) alias.scripts.get(0)).scripts.get(0).name);
        assertEquals("resolve", ((Alias) alias.scripts.get(0)).scripts.get(0).unparsedName);
        assertEquals(Scope.Default, ((Alias) alias.scripts.get(0)).scripts.get(0).scope);
        assertTrue(((Alias) alias.scripts.get(0)).scripts.get(0).getClass() == Alias.class);
        assertEquals(1, ((Alias) ((Alias) alias.scripts.get(0)).scripts.get(0)).scripts.size());
        assertEquals("resolve-1.0.jar", ((Alias) ((Alias) alias.scripts.get(0)).scripts.get(0)).scripts.get(0).name);
        assertEquals("resolve-1.0.jar", ((Alias) ((Alias) alias.scripts.get(0)).scripts.get(0)).scripts.get(0).unparsedName);
        assertEquals(Scope.Default, ((Alias) ((Alias) alias.scripts.get(0)).scripts.get(0)).scripts.get(0).scope);
        assertTrue(((Alias) ((Alias) alias.scripts.get(0)).scripts.get(0)).scripts.get(0).getClass() == Script.class);
        assertEquals("clean-1.0.jar", ((Alias) alias.scripts.get(0)).scripts.get(1).name);
        assertEquals("clean-1.0.jar", ((Alias) alias.scripts.get(0)).scripts.get(1).unparsedName);
        assertEquals(Scope.Default, ((Alias) alias.scripts.get(0)).scripts.get(1).scope);
        assertTrue(((Alias) alias.scripts.get(0)).scripts.get(1).getClass() == Script.class);
        assertEquals("resolve", alias.scripts.get(1).name);
        assertEquals("resolve", alias.scripts.get(1).unparsedName);
        assertEquals(Scope.Default, alias.scripts.get(1).scope);
        assertTrue(alias.scripts.get(1).getClass() == Alias.class);
        assertEquals(1, ((Alias) alias.scripts.get(1)).scripts.size());
        assertEquals("resolve-1.0.jar", ((Alias) alias.scripts.get(1)).scripts.get(0).name);
        assertEquals("resolve-1.0.jar", ((Alias) alias.scripts.get(1)).scripts.get(0).unparsedName);
        assertTrue(((Alias) alias.scripts.get(1)).scripts.get(0).getClass() == Script.class);
        assertEquals(Scope.Default, ((Alias) alias.scripts.get(1)).scripts.get(0).scope);
        assertEquals("compiler.jar", alias.scripts.get(2).name);
        assertEquals("compiler.jar", alias.scripts.get(2).unparsedName);
        assertEquals(Scope.Default, alias.scripts.get(2).scope);
        assertTrue(alias.scripts.get(2).getClass() == Script.class);

        // test that ad-hoc props defined within an alias are recognized.
        String scope = String.valueOf(System.currentTimeMillis());
        PropFileChain chain = Props.get(Context.named(scope), Scope.named(scope),  new File("./ply/config"));
        assertEquals(Prop.Empty, chain.get("clean"));
        name = "clean";
        value = "\"rm -rf target\" -Pply.color=false";
        unparsedAliases.clear();
        List<String> adHocProps = new ArrayList<String>(1);
        adHocProps.add(scope + "#" + scope + ".test=hello");
        container = new PropFile(Context.named("aliases"), PropFile.Loc.Local);
        unparsedAliases.put(name, container.add(name, value));
        alias = resolver.parseAlias(Scope.Default, Script.parse(name, Scope.Default), value, unparsedAliases,
                                 new DirectedAcyclicGraph<String>(), new HashMap<String, Alias>(), adHocProps);
        assertEquals("clean", alias.name);
        assertEquals("clean", alias.unparsedName);
        assertEquals(1, alias.scripts.size());
        assertEquals("rm", alias.scripts.get(0).name);
        assertEquals(2, alias.scripts.get(0).arguments.size());
        assertEquals("-rf", alias.scripts.get(0).arguments.get(0));
        assertEquals("target", alias.scripts.get(0).arguments.get(1));
        assertEquals("rm -rf target", alias.scripts.get(0).unparsedName);
        assertEquals(2, alias.adHocProps.size());
        String testProp = alias.adHocProps.get(0);
        assertEquals(scope + "#" + scope + ".test=hello", testProp);
        String colorProp = alias.adHocProps.get(1);
        assertEquals("ply.color=false", colorProp);

        // test multiple scopes processed in one run (can lead to stack-overflows if not coded correctly)
        name = "example";
        value = "dep add foo:bar:1.0";
        unparsedAliases.clear();
        container = new PropFile(Context.named("aliases"), PropFile.Loc.Local);
        unparsedAliases.put(name, container.add(name, value));
        alias = resolver.parseAlias(Scope.Default, Script.parse(name, Scope.Default), value, unparsedAliases,
                                 new DirectedAcyclicGraph<String>(), new HashMap<String, Alias>(), new ArrayList<String>());
        assertEquals("example", alias.name);
        assertEquals("example", alias.unparsedName);
        assertEquals(3, alias.scripts.size());
        assertEquals("dep", alias.scripts.get(0).name);
        assertEquals("dep", alias.scripts.get(0).unparsedName);
        assertEquals("add", alias.scripts.get(1).name);
        assertEquals("add", alias.scripts.get(1).unparsedName);
        assertEquals("bar:1.0", alias.scripts.get(2).name);
        assertEquals("foo:bar:1.0", alias.scripts.get(2).unparsedName);
        assertEquals("foo", alias.scripts.get(2).scope.name);
    }

}
