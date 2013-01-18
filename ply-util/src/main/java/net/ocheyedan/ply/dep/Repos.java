package net.ocheyedan.ply.dep;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.SystemExit;
import net.ocheyedan.ply.props.*;

import java.io.File;
import java.util.*;

/**
 * User: blangel
 * Date: 1/4/13
 * Time: 11:31 AM
 *
 * Utility class to interact with {@literal repository} urls.
 */
public final class Repos {

    /**
     * The repository authorization types supported by ply.
     */
    public static enum AuthType {

        /* uses a git repo as a ply repository */
        git;

        public Auth get(String username, String encryptedPwd, RepositoryAtom atom, File configDir, Scope scope) {
            switch (this) {
                case git:
                    return new GitHubAuth(username, encryptedPwd, atom, configDir, scope);
                default:
                    throw new AssertionError("Unknown repo auth-type");
            }
        }
    }

    /**
     * Copies the project artifact ({@literal project.artifact.name} from {@literal project.build.dir}) into
     * {@code localRepo}
     * @param localRepo into which to install the artifact
     * @return true on success, false if the artifact does not exist or there was an error saving
     */
    public static boolean install(RepositoryAtom localRepo) {

        String buildDirPath = Props.get("build.dir", Context.named("project")).value();
        String artifactName = Props.get("artifact.name", Context.named("project")).value();
        File artifact = FileUtil.fromParts(buildDirPath, artifactName);
        if (!artifact.exists()) {
            return false;
        }

        String plyProjectDirPath = Props.get("project.dir", Context.named("ply")).value();
        File dependenciesFile = FileUtil.fromParts(plyProjectDirPath, "config", "dependencies.properties");

        String namespace = Props.get("namespace", Context.named("project")).value();
        String name = Props.get("name", Context.named("project")).value();
        String version = Props.get("version", Context.named("project")).value();
        String convertedNamespace = (localRepo.isPlyType() ? namespace : namespace.replaceAll("\\.", File.separator));
        String localRepoPath = Deps.getDirectoryPathForRepo(localRepo);
        String localRepoArtifactBasePath = FileUtil.pathFromParts(localRepoPath, convertedNamespace, name, version);
        File localRepoArtifact = FileUtil.fromParts(localRepoArtifactBasePath, artifactName);
        FileUtil.copy(artifact, localRepoArtifact);

        File localRepoDependenciesFile = FileUtil.fromParts(localRepoArtifactBasePath, "dependencies.properties");
        if (dependenciesFile.exists()) {
            return FileUtil.copy(dependenciesFile, localRepoDependenciesFile);
        } else {
            // need to override (perhaps there were dependencies but now none.
            PropFile dependencies = new PropFile(Context.named("dependencies"), PropFile.Loc.Local);
            return PropFiles.store(dependencies, localRepoDependenciesFile.getPath(), true);
        }
    }

    /**
     * Constructs a {@link RepositoryRegistry} for all repositories defined within the {@code configDirectory} and
     * {@code scope}.  If {@code syntheticDependencyKey} is not null maps it to {@code syntheticDependencies} as a
     * synthetic repository within the returned {@link RepositoryRegistry}.
     * @param configDirectory for which to pull the repositories properties
     * @param scope for which to pull the repositories properties
     * @param syntheticDependencyKey if not null the dependency key into a synthetic repository containing
     *                               {@code syntheticDependencies}
     * @param syntheticDependencies the dependencies to be used if {@code syntheticDependencyKey} is not null
     * @return a {@link RepositoryRegistry} containing all the {@link RepositoryAtom} values
     *         defined by the {@literal repositories.properties} for this invocation.
     * @throws SystemExit if the local repository cannot be found.
     */
    public static RepositoryRegistry createRepositoryRegistry(File configDirectory, Scope scope,
                                                              DependencyAtom syntheticDependencyKey,
                                                              List<DependencyAtom> syntheticDependencies)
            throws SystemExit {
        PropFile.Prop localRepoProp = Props.get("localRepo", Context.named("depmngr"), scope, configDirectory);
        RepositoryAtom localRepo = RepositoryAtom.parse(localRepoProp.value());
        if (localRepo == null) {
            if (PropFile.Prop.Empty.equals(localRepoProp)) {
                Output.print("^error^ No ^b^localRepo^r^ property defined (^b^ply set localRepo=xxxx in depmngr^r^).");
            } else {
                Output.print("^error^ Could not resolve directory for ^b^localRepo^r^ property [ is ^b^%s^r^ ].", localRepoProp.value());
            }
            throw new SystemExit(1);
        }
        List<RepositoryAtom> repositoryAtoms = new ArrayList<RepositoryAtom>();
        PropFileChain repositories = Props.get(Context.named("repositories"), scope, configDirectory);
        if (repositories != null) {
            for (PropFile.Prop repoProp : repositories.props()) {
                String repoUri = repoProp.name;
                if (localRepo.getPropertyName().equals(repoUri)) {
                    continue;
                }
                String repoType = repoProp.value();
                String repoAtom = repoType + ":" + repoUri;
                RepositoryAtom repo = RepositoryAtom.parse(repoAtom);
                if (repo == null) {
                    Output.print("^warn^ Invalid repository declared %s, ignoring.", repoAtom);
                } else {
                    Auth auth = getAuth(configDirectory, scope, repo);
                    repo.setAuth(auth);
                    repositoryAtoms.add(repo);
                }
            }
        }
        Collections.sort(repositoryAtoms, RepositoryAtom.LOCAL_COMPARATOR);
        Map<DependencyAtom, List<DependencyAtom>> synthetic = null;
        if (syntheticDependencyKey != null) {
            synthetic = new HashMap<DependencyAtom, List<DependencyAtom>>(1);
            synthetic.put(syntheticDependencyKey, syntheticDependencies);
        }
        return new RepositoryRegistry(localRepo, repositoryAtoms, synthetic);
    }

    /**
     * Resolves {@code repository} to a {@link RepositoryAtom}
     * @param configDir for property resolution
     * @param scope for property resolution
     * @param repository to resolve
     * @return null if {@code repository} is parsable as a {@link RepositoryAtom} but is not an existing
     *         repository entry
     * @throws SystemExit on parsing of {@code repository} failure
     */
    public static RepositoryAtom getExistingRepo(File configDir, Scope scope, String repository) throws SystemExit {
        RepositoryAtom atom = RepositoryAtom.parse(repository);
        if (atom == null) {
            Output.print("^error^ Repository %s not of format [type:]repoUri.", repository);
            throw new SystemExit(1);
        }

        PropFile.Prop found = Props.get(atom.getPreResolvedUri(), Context.named("repositories"), scope, configDir);
        if (PropFile.Prop.Empty.equals(found)) {
            // before failing, check that this isn't a resolved unix-tilde path
            String toTry = FileUtil.reverseUnixTilde(atom.getPreResolvedUri());
            RepositoryAtom toTryAtom = RepositoryAtom.parse(toTry);
            found = (toTryAtom == null ? PropFile.Prop.Empty :
                    Props.get(toTryAtom.getPreResolvedUri(), Context.named("repositories"), scope, configDir));
            if (PropFile.Prop.Empty.equals(found)) {
                Output.print("^warn^ Repository not found; given %s", repository);
                return null;
            } else {
                atom = toTryAtom;
            }
        }
        return atom;
    }

    /**
     * @param configDir from which to resolve properties
     * @param scope from which to resolve properties
     * @param repositoryAtom to which to match the associated entry within {@link Context} {@literal repomngr}
     * @return the {@link PropFile.Prop} within {@link Context} {@literal repomngr} associated with {@code repositoryAtom}
     */
    public static PropFile.Prop getAuthPropFromRepo(File configDir, Scope scope, RepositoryAtom repositoryAtom) {
        String repository = repositoryAtom.getPreResolvedUri();
        PropFile.Prop prop = Props.get(repository, Context.named("repomngr"), scope, configDir);
        if (PropFile.Prop.Empty.equals(prop)) {
            String toTry;
            if (repository.startsWith("~")) {
                toTry = FileUtil.resolveUnixTilde(repository);
            } else if (repository.startsWith("/")) {
                toTry = FileUtil.reverseUnixTilde(repository);
            } else {
                return prop;
            }
            return Props.get(toTry, Context.named("repomngr"), scope, configDir);
        }
        return prop;
    }

    /**
     * Constructs an authorization property for the {@code repomngr} where the {@code repository} is the property
     * key and the property value is the concatenation (separated with colon characters) of {@code auth.authType},
     * {@code auth.username} and {@code auth.encryptedPwd}.
     * @param repomngr in which to add the property entry
     * @param repository property key
     * @param auth authorization information
     * @return the created {@link PropFile.Prop} value
     */
    public static PropFile.Prop addAuthRepomngrProp(PropFile repomngr, String repository, Auth auth) {
        String propertyValue = auth.getPropertyValue();
        if (repomngr.contains(repository)) {
            repomngr.remove(repository);
        }
        return repomngr.add(repository, propertyValue);
    }

    /**
     * @param configDir for which to load properties
     * @param scope for which to load properties
     * @param repositoryAtom the repository for which to look for authentication information
     * @return the {@link Auth} associated with {@code repositoryAtom} or null if none exists or there were issues parsing
     *         the stored value
     */
    public static Auth getAuth(File configDir, Scope scope, RepositoryAtom repositoryAtom) {
        String repository = repositoryAtom.getPreResolvedUri();
        PropFile.Prop prop = getAuthPropFromRepo(configDir, scope, repositoryAtom);
        if (PropFile.Prop.Empty.equals(prop)) {
            return null;
        } else {
            String value = prop.value();
            int index = value.indexOf(':');
            if (index == -1) {
                Output.print("^warn^ Found auth setting for repo [ %s ] but could not parse it [ %s ], ignoring", repository, value);
                return null;
            }
            String type = value.substring(0, index);
            AuthType authType;
            try {
                authType = AuthType.valueOf(type);
            } catch (Exception e) {
                Output.print("^warn^ Found auth setting for repo [ %s ] but invalid auth-type [ %s ], ignoring", repository, value);
                return null;
            }
            int usernameIndex = value.indexOf(':', index + 1);
            if (usernameIndex == -1) {
                Output.print("^warn^ Found auth setting for repo [ %s ] but could not parse it [ %s ], ignoring", repository, value);
                return null;
            }
            String username = value.substring(index + 1, usernameIndex);
            if (usernameIndex >= (value.length() - 1)) {
                Output.print("^warn^ Found auth setting for repo [ %s ] but could not parse it [ %s ], ignoring", repository, value);
                return null;
            }
            String encryptedPwd = value.substring(usernameIndex + 1);
            return authType.get(username, encryptedPwd, repositoryAtom, configDir, scope);
        }
    }

    private Repos() { }

}