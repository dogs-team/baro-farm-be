FROM public.ecr.aws/bitnami/elasticsearch:9.1.2-debian-12-r0

# Install analysis-nori plugin for Korean text analysis.
# Bitnami's chart copies plugins from ELASTICSEARCH_DEFAULT_PLUGINS_DIR
# into the runtime plugins emptyDir via the copy-default-plugins init container,
# so we must ensure the default plugins directory is populated.
USER 0
RUN mkdir -p /opt/bitnami/elasticsearch/plugins.default \
    && /opt/bitnami/elasticsearch/bin/elasticsearch-plugin install --batch analysis-nori \
    && cp -R /opt/bitnami/elasticsearch/plugins/analysis-nori /opt/bitnami/elasticsearch/plugins.default/

# Use the default non-root user provided by the Bitnami image
USER 1001

