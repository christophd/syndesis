import { getConnectionIcon, WithConnectors } from '@syndesis/api';
import { Connector, IConnectionWithIconFile } from '@syndesis/models';
import {
  ConnectionCard,
  ConnectionCreatorLayout,
  ConnectionsGridCell,
  ConnectionSkeleton,
  IActiveFilter,
  IFilterType,
  ISortType,
  ListViewToolbar,
  PageSection,
} from '@syndesis/ui';
import { WithListViewToolbarHelpers, WithLoader } from '@syndesis/utils';
import * as React from 'react';
import { Translation } from 'react-i18next';
import i18n from '../../../../i18n';
import { ApiError, PageTitle } from '../../../../shared';
import { ConnectionCreatorBreadSteps } from '../../components';
import { ConnectionCreatorBreadcrumb } from '../../components/ConnectionCreatorBreadcrumb';
import resolvers from '../../resolvers';

function getFilteredAndSortedConnectors(
  connections: Connector[],
  activeFilters: IActiveFilter[],
  currentSortType: ISortType,
  isSortAscending: boolean
) {
  let filteredAndSortedConnections = connections;
  activeFilters.forEach((filter: IActiveFilter) => {
    const valueToLower = filter.value.toLowerCase();
    filteredAndSortedConnections = filteredAndSortedConnections.filter(
      (c: Connector) => c.name.toLowerCase().includes(valueToLower)
    );
  });

  filteredAndSortedConnections = filteredAndSortedConnections.sort(
    (miA, miB) => {
      const left = isSortAscending ? miA : miB;
      const right = isSortAscending ? miB : miA;
      return left.name.localeCompare(right.name);
    }
  );

  return filteredAndSortedConnections;
}

const filterByName = {
  filterType: 'text',
  id: 'name',
  placeholder: i18n.t('shared:filterByNamePlaceholder'),
  title: i18n.t('shared:Name'),
} as IFilterType;

const filterTypes = [filterByName];

const sortByName = {
  id: 'name',
  isNumeric: false,
  title: i18n.t('shared:Name'),
} as ISortType;

const sortTypes: ISortType[] = [sortByName];

export default class ConnectorsPage extends React.Component {
  public render() {
    return (
      <Translation ns={['connections', 'shared']}>
        {t => (
          <WithConnectors>
            {({ data, hasData, error }) => (
              <>
                <PageTitle title={t('connections:create:connector:title')} />
                <ConnectionCreatorBreadcrumb
                  cancelHref={resolvers.connections()}
                />
                <ConnectionCreatorLayout
                  header={<ConnectionCreatorBreadSteps step={1} />}
                  content={
                    <WithLoader
                      error={error}
                      loading={!hasData}
                      loaderChildren={
                        <PageSection>
                          {new Array(5).fill(0).map((_, index) => (
                            <ConnectionsGridCell key={index}>
                              <ConnectionSkeleton />
                            </ConnectionsGridCell>
                          ))}
                        </PageSection>
                      }
                      errorChildren={<ApiError />}
                    >
                      {() => (
                        <WithListViewToolbarHelpers
                          defaultFilterType={filterByName}
                          defaultSortType={sortByName}
                        >
                          {helpers => {
                            const filteredAndSortedConnectors = getFilteredAndSortedConnectors(
                              data.connectorsForDisplay,
                              helpers.activeFilters,
                              helpers.currentSortType,
                              helpers.isSortAscending
                            );

                            return (
                              <>
                                <PageSection noPadding={true} variant={'light'}>
                                  <ListViewToolbar
                                    filterTypes={filterTypes}
                                    sortTypes={sortTypes}
                                    resultsCount={
                                      filteredAndSortedConnectors.length
                                    }
                                    {...helpers}
                                    i18nResultsCount={t('shared:resultsCount', {
                                      count: filteredAndSortedConnectors.length,
                                    })}
                                  />
                                </PageSection>
                                <PageSection>
                                  {filteredAndSortedConnectors.map(
                                    (connector, index) => {
                                      const isTechPreview =
                                        connector!.metadata! &&
                                        connector!.metadata!['tech-preview'] ===
                                          'true';

                                      return (
                                        <ConnectionsGridCell key={index}>
                                          <ConnectionCard
                                            configurationRequired={false}
                                            name={connector.name}
                                            description={
                                              connector.description || ''
                                            }
                                            i18nCannotDelete={t('cannotDelete')}
                                            i18nConfigurationRequired={t(
                                              'configurationRequired'
                                            )}
                                            i18nTechPreview={t('techPreview')}
                                            icon={getConnectionIcon(
                                              process.env.PUBLIC_URL,
                                              connector as IConnectionWithIconFile
                                            )}
                                            href={resolvers.create.configureConnector(
                                              {
                                                connector,
                                              }
                                            )}
                                            techPreview={isTechPreview}
                                            techPreviewPopoverHtml={
                                              <span
                                                dangerouslySetInnerHTML={{
                                                  __html: t(
                                                    'techPreviewPopoverHtml'
                                                  ),
                                                }}
                                              />
                                            }
                                          />
                                        </ConnectionsGridCell>
                                      );
                                    }
                                  )}
                                </PageSection>
                              </>
                            );
                          }}
                        </WithListViewToolbarHelpers>
                      )}
                    </WithLoader>
                  }
                />
              </>
            )}
          </WithConnectors>
        )}
      </Translation>
    );
  }
}
