package de.saumya.mojo.gem;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * goal to nexus push a given gem or a gem artifact to rubygems.org via the 
 * command "gem nexus {gemfile}"
 * 
 * @goal nexus
 * @phase deploy
 */
public class NexusMojo extends AbstractGemMojo {

    /**
     * skip the pushng the gem
     * <br/>
     * Command line -Dnexus.skipp=...
     * 
     * @parameter expression="${nexus.skip}"
     */
    protected boolean skip = false;

    /**
     * arguments for the ruby script given through file parameter.
     * <br/>
     * Command line -Dnexus.args=...
     * 
     * @parameter expression="${nexus.args}"
     */
    protected String nexusArgs = null;

    /**
     * arguments for the ruby script given through file parameter.
     * <br/>
     * Command line -Dgem=...
     * 
     * @parameter expression="${gem}"
     */
    protected File gem;
    
    /**
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    protected Object repoSession;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException{
        if (skip){
            getLog().info( "skipping to nexus push gem" );
        }
        else {
            super.execute();
        }
    }
    
    @Override
    public void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException, MojoFailureException, GemException {
        if ( getJrubyVersion().needsOpenSSL() ) {
            gemsInstaller.installOpenSSLGem(this.repoSession, localRepository, getRemoteRepos() );
        }
        final Script script = this.factory.newScriptFromJRubyJar("gem")
                .addArg("nexus");
        if(this.project.getArtifact().getFile() == null){
            File f = new File(this.project.getBuild().getDirectory(), this.project.getBuild().getFinalName() +".gem");
            if (f.exists()) {
                this.project.getArtifact().setFile(f);
            }
        }
        // no given gem and pom artifact in place
        if (this.gem == null && this.project.getArtifact() != null
                && this.project.getArtifact().getFile() != null
                && this.project.getArtifact().getFile().exists()) {
            final GemArtifact gemArtifact = new GemArtifact(this.project);
            // skip artifact unless it is a gem.
            // this allows to use this mojo for installing arbitrary gems
            if (!gemArtifact.isGem()) {
                throw new MojoExecutionException("not a gem artifact");
            }
            script.addArg(gemArtifact.getFile());
        }
        else {
            // no pom artifact and no given gem so search for a gem
            if (this.gem == null && null == args ) {
                for (final File f : this.launchDirectory().listFiles()) {
                    if (f.getName().endsWith(".gem")) {
                        if (this.gem == null) {
                            this.gem = f;
                        }
                        else {                            
                            throw new MojoFailureException("more than one gem file found, use -Dgem=... to specifiy one");
                        }
                    }
                }
            }
            if (this.gem != null) {
                getLog().info("use gem: " + this.gem);
                script.addArg(this.gem);
            }
        }
        script.addArgs(this.nexusArgs);
        script.addArgs(this.args);
        script.execute();
    }
}
